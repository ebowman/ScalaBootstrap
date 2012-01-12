package com.gilt.hackathon

import java.io.File
import org.slf4j.LoggerFactory
import akka.actor.Actor
import scala.concurrent.ops.spawn
import java.net.{Socket, ServerSocket}
import java.util.concurrent.{CountDownLatch, Executors}

object Server extends App {
  val playlistPort = args.headOption.map(_.toInt).getOrElse(8080)
  val radioPort = args.drop(1).headOption.map(_.toInt).getOrElse(8181)
  val dir = args.drop(2).headOption.map(new File(_)).getOrElse(new File("."))
  val files = find(dir, _.getName.endsWith(".mp3"))
  val log = LoggerFactory.getLogger(getClass)
  val chunkSize = 32 * 1024
  val executor = Executors.newScheduledThreadPool(Runtime.getRuntime.availableProcessors)

  server(playlistPort) { socket =>
    val actor = Actor.actorOf(new PlaylistActor(socket, files)).start()
    actor ! StartStreaming
  }

  val radioStation = Actor.actorOf(new RadioStation(files)).start()
  radioStation ! NextChunk
  server(radioPort) { socket =>
    val listener = Actor.actorOf(new RadioListener(socket)).start()
    listener ! ListenTo(radioStation)
  }

  val latch = new CountDownLatch(1)
  latch.await()

  def server(port: Int)(f: Socket => Unit) {
    spawn {
      try {
        while (true) {
          log.info("Starting server on port " + port)
          val serverSocket = new ServerSocket(port)
          try {
            while (true) {
              val socket = serverSocket.accept()
              log.info("Accepted new connection: " + socket.getRemoteSocketAddress)
              f(socket)
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
      } catch {
        case e => e.printStackTrace()
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



