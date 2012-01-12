package com.gilt.hackathon

import java.net.Socket
import java.io.{IOException, OutputStream, ByteArrayOutputStream, File}
import akka.actor.{PoisonPill, ActorRef, Actor}
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

case class Subscribe(actor: ActorRef)

case class Unsubscribe(actor: ActorRef)

case class ListenTo(station: ActorRef)
case class Chunk(bytes: Array[Byte])

object RadioListener {
  val log = LoggerFactory.getLogger(classOf[RadioListener])
}

class RadioListener(socket: Socket) extends Actor {
  import RadioListener.log
  private var output: OutputStream = _
  private var station: ActorRef = _
  def receive = {
    case msg @ ListenTo(_) =>
      withCleanup {
        new LineIterator(socket.getInputStream).takeWhile(_ != "")
        station = msg.station
        station ! Subscribe(self)
        output = socket.getOutputStream
        IcyResponder.writeResponseHeaders(output)
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
        log.info("Shutting down listener: " + e.toString)
        station ! Unsubscribe(self)
        self ! PoisonPill
        socket.close()
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
      subscribers.foreach((a: ActorRef) => a ! Chunk(chunk.toByteArray))
      Server.executor.schedule(() => self ! NextChunk, sleep, TimeUnit.MILLISECONDS)
  }
}
