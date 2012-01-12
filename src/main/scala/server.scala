package com.gilt.hackathon

import java.io.File
import org.slf4j.LoggerFactory
import akka.actor.Actor
import java.net.ServerSocket
import java.util.concurrent.Executors

object Server extends App {
  val port = args.headOption.map(_.toInt).getOrElse(8080)
  val dir = args.drop(1).headOption.map(new File(_)).getOrElse(new File("."))
  val files = find(dir, _.getName.endsWith(".mp3"))
  val log = LoggerFactory.getLogger(getClass)
  val chunkSize = 32 * 1024
  val executor = Executors.newScheduledThreadPool(Runtime.getRuntime.availableProcessors)

  while (true) {
    val serverSocket = new ServerSocket(port)
    try {
      while (true) {
        log.info("Starting server on port " + port)
        val socket = serverSocket.accept()
        val actor = Actor.actorOf(new RequestActor).start()
        actor ! StartStreaming(socket, files)
      }
    } catch {
      case e =>
        e.printStackTrace()
        try {
          serverSocket.close()
        } catch {
          case _ => // ignore
        }
    }
  }

  def find(dir: File, filterFunc: (File => Boolean)): Iterable[File] = {
    require(dir.exists && dir.isDirectory)
    def recurse(aDir: File): Iterable[File] = {
      val files = aDir.listFiles
      files.filter(_.isFile).filter(filterFunc) ++ files.filter(_.isDirectory).flatMap(recurse)
    }
    recurse(dir)
  }
}



