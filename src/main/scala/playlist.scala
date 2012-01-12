package com.gilt.hackathon

import java.net.Socket
import org.slf4j.LoggerFactory
import java.io.{IOException, OutputStream, File}
import java.util.concurrent.TimeUnit
import akka.actor.{PoisonPill, Actor}

// sent to a PlaylistActor to tell it to start streaming.
// We send a message instead of doing it in the constructor
// so that this work happens on the actor threadpool, not
// the main server thread
case object StartStreaming

// message to tell the actor to send the next chunk
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
      withCleanup {
        new LineIterator(socket.getInputStream).takeWhile(_ != "")
        output = socket.getOutputStream
        responder = new IcyResponder(new ChunkIterator(Server.chunkSize, files))
        IcyResponder.writeResponseHeaders(output, "scala playlist streamer", Server.playlistPort)
        responder.writeNextChunk(output) // write the first chunk quickly, the client is waiting
        self ! NextChunk
      }
    case NextChunk =>
      withCleanup {
        val sleep = responder.writeNextChunk(output)
        Server.executor.schedule(() => self ! NextChunk, sleep, TimeUnit.MILLISECONDS)
      }
  }

  private def withCleanup(f: => Unit) {
    try {
      f
    } catch {
      case e: IOException =>
        log.error("Unexpected exception; closing socket: " + e)
        self ! PoisonPill
        socket.close()
      case e => e.printStackTrace()
    }
  }
}
