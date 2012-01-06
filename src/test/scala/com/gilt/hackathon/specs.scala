package com.gilt.hackathon

import org.scalatest.Spec
import java.net.URL
import akka.actor.{Actor, Props, ActorSystem}
import java.util.concurrent.CountDownLatch
import org.scalatest.matchers.ShouldMatchers

class SlaveSpec extends Spec with ShouldMatchers {
  describe("A Slave actor") {
    it("should be able to find a known string in a known uri") {
      val system = ActorSystem("SlaveSpec")
      val slave = system.actorOf(Props(new Slave))
      var finalItems: Traversable[Entry] = null
      val latch = new CountDownLatch(1)
      system.actorOf(Props(new Actor {
        slave ! SearchTask(new URL("file:src/test/resources/atom.xml"), "hello")

        def receive = {
          case Result(url, items) =>
            finalItems = items
            latch.countDown()

        }
      }))
      latch.await()
      system.shutdown()
      finalItems.head.title should equal("Dita Eyewear")
    }
  }
}

class MasterSpec extends Spec {
  describe("A Master actor") {
    it("should be able to spawn children and find known results") {

    }
  }
}
