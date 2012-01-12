package com.gilt.hackathon

import java.net.Socket
import java.io.{IOException, OutputStream, ByteArrayOutputStream, File}
import akka.actor.{PoisonPill, ActorRef, Actor}
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

// tells a RadioStation to subscribe this RadioListener
case class Subscribe(actor: ActorRef)

// tells a RadioStation to unsubscribe this RadioListener
case class Unsubscribe(actor: ActorRef)

// tells a RadioListener to listen to this station
case class ListenTo(station: ActorRef)

// tells a RadioListener to push these bytes to its listener socket
case class Chunk(bytes: Array[Byte])

object RadioListener {
  val log = LoggerFactory.getLogger(classOf[RadioListener])
}

class RadioListener(socket: Socket) extends Actor {
  import RadioListener.log
  private var output: OutputStream = _
  private var station: ActorRef = _
  private var dead = false

  def receive = {
    case ListenTo(station) =>
      withCleanup {
        this.station = station
        new LineIterator(socket.getInputStream).takeWhile(_ != "")
        station ! Subscribe(self)
        output = socket.getOutputStream
        IcyResponder.writeResponseHeaders(output, "scala radio streamer", Server.radioPort)
      }
    case Chunk(bytes) =>
      withCleanup {
        output.write(bytes)
      }
  }

  def withCleanup(f: => Unit) {
    try {
      f
    } catch {
      case e: IOException =>
        if (!dead) {
          log.info("Shutting down listener " + socket.getRemoteSocketAddress + ": " + e.toString)
          dead = true
          station ! Unsubscribe(self)
          self ! PoisonPill
          socket.close()
        }
    }
  }
}

class RadioStation(files: Iterable[File]) extends Actor {

  import ImplicitHelpers._

  val subscribers = collection.mutable.Set[ActorRef]()
  val responder = new IcyResponder(new ChunkIterator(Server.chunkSize, files))

  def receive = {
    case Subscribe(actor) =>
      subscribers += actor
    case Unsubscribe(actor) =>
      subscribers -= actor
    case NextChunk =>
      val chunk = new ByteArrayOutputStream
      val sleep = responder.writeNextChunk(chunk)
      val bytes = chunk.toByteArray
      subscribers.foreach((a: ActorRef) => a ! Chunk(bytes))
      Server.executor.schedule(() => self ! NextChunk, sleep, TimeUnit.MILLISECONDS)
  }
}
