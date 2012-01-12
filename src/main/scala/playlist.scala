package com.gilt.hackathon

import java.net.Socket
import org.slf4j.LoggerFactory
import java.io.{IOException, OutputStream, File}
import java.util.concurrent.TimeUnit
import akka.actor.{PoisonPill, Actor}

case object StartStreaming

case object NextChunk

object PlaylistActor {
  val log = LoggerFactory.getLogger(classOf[PlaylistActor])
}


class PlaylistActor(socket: Socket, files: Iterable[File]) extends Actor {

  import PlaylistActor.log
  import ImplicitHelpers._

  var responder: IcyResponder = _
  var output: OutputStream = _

  def receive = {
    case StartStreaming =>
      try {
        new LineIterator(socket.getInputStream).takeWhile(_ != "")
        output = socket.getOutputStream
        responder = new IcyResponder(new ChunkIterator(Server.chunkSize, files))
        IcyResponder.writeResponseHeaders(output)
        responder.writeNextChunk(output) // write the first chunk quickly, the client is waiting
        self ! NextChunk
      } catch {
        case e: IOException =>
          log.error("Unexpected exception; closing socket: " + e)
          self ! PoisonPill
          socket.close()
        case e => e.printStackTrace()
      }
    case NextChunk =>
      try {
        val sleep = responder.writeNextChunk(output)
        Server.executor.schedule(() => self ! NextChunk, sleep, TimeUnit.MILLISECONDS)
      } catch {
        case e: IOException =>
          log.error("Unexpected exception; closing socket: " + e)
          self ! PoisonPill
          socket.close()
        case e => e.printStackTrace()
      }
  }
}
