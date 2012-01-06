package com.gilt.hackathon

import akka.actor.{Actor, Props, ActorSystem}
import java.util.concurrent.CountDownLatch
import xml.XML
import java.net.URL


object SimpleAtomSearch extends App {

  val system = ActorSystem("SimpleAtomSearch")

  val latch = new CountDownLatch(1)

  val master = system.actorOf(Props(new Master(latch)))
  println("Searching for %s" format args(0))
  master ! Search(args(0))
  latch.await()
  system.shutdown()

  def sanitize(text: String): String = {
    text.replaceAll("""<\/?\w+((\s+\w+(\s*=\s*(?:".*?"|'.*?'|[^'">\s]+))?)+\s*|\s*)\/?>""", " ").replaceAll("""[\s\n\r]+""", " ")
  }
}

case class Entry(title: String, description: String, url: String)

case class Search(query: String)

case class SearchTask(url: URL, query: String)

case class Result(url: URL, items: Traversable[Entry])

class Master(latch: CountDownLatch) extends Actor {
  val urls = Seq("https://api.gilt.com/v1/sales/men/active.atom",
    "https://api.gilt.com/v1/sales/men/upcoming.atom",
    "https://api.gilt.com/v1/sales/women/active.atom",
    "https://api.gilt.com/v1/sales/women/upcoming.atom",
    "https://api.gilt.com/v1/sales/kids/active.atom",
    "https://api.gilt.com/v1/sales/kids/upcoming.atom",
    "https://api.gilt.com/v1/sales/home/active.atom",
    "https://api.gilt.com/v1/sales/home/upcoming.atom")
  var pending = urls.size
  val results = collection.mutable.Set[Entry]()

  def receive = {
    case Search(query) =>
      urls foreach {
        url => SimpleAtomSearch.system.actorOf(Props(new Slave)) ! SearchTask(new URL(url), query)
      }
    case Result(url, items) =>
      pending -= 1
      results ++= items
      if (pending == 0) {
        results.map(e => e.title + " -> " + e.url).toList.distinct.foreach(println)
        latch.countDown()
      }
  }
}

class Slave extends Actor {
  def receive = {
    case SearchTask(url, query) =>
      val atomXml = XML.load(url)
      val result = for {
        entryXml <- atomXml \\ "entry"
        description = SimpleAtomSearch.sanitize((entryXml \ "content").text) if description.toLowerCase.contains(query)
      } yield {
        Entry((entryXml \ "title").text, description, (entryXml \\ "link").head.attribute("href").get.head.text)
      }
      sender ! Result(url, result)
  }
}
