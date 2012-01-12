package com.gilt.hackathon

import java.net.Socket
import org.slf4j.LoggerFactory
import akka.actor.{PoisonPill, Actor}
import java.io.{IOException, OutputStream, File}
import java.util.concurrent.TimeUnit

case object StartStreaming

case class NextChunk(chunks: ChunkIterator)

object RequestActor {
  val log = LoggerFactory.getLogger(classOf[RequestActor])
}

class RequestActor(socket: Socket, files: Iterable[File]) extends Actor {
  import RequestActor.log

  val output = socket.getOutputStream

  def receive = {
    case StartStreaming =>
      try {
        // block & suck until blank line (end of headers)
        // (later we might like to look at these headers...)
        new LineIterator(socket.getInputStream).dropWhile(_ != "")
        val output = socket.getOutputStream
        writeResponseHeaders(output)
        val chunks = new ChunkIterator(Server.chunkSize, files)
        writeNextChunk(chunks)  // write the first chunk quickly, the client is waiting
        self ! NextChunk(chunks)
      } catch {
        case e: IOException =>
          log.error("Unexpected exception", e)
          socket.close()
      }
    case NextChunk(chunks) =>
      writeNextChunk(chunks)
  }

  def writeNextChunk(chunks: ChunkIterator) {
    try {
      val startTime = System.currentTimeMillis
      val chunk = chunks.next()
      output.write(chunk)
      val metadata = generateMetadata(chunks.curStatus)
      val marker = (metadata.length / 16).asInstanceOf[Byte]
      output.write(marker)
      output.write(metadata.getBytes("US-ASCII"))
      val duration = System.currentTimeMillis - startTime
      val bitrate = chunks.curBitRate
      // we may wish to stream slightly faster than the theoretical minimum
      val realityFactor = 1.0f
      // effectiveBitsPerSecond = 8000f * chunk.length / duration
      // bitrate = 8000f * chunk.length / (duration + sleep)
      // bitrate*duration + bitrate*sleep = 8000f * chunk.length
      // bitrate * sleep = 8000f * chunk.length - bitrate * duration
      // sleep = (8000f * chunk.length - bitrate * duration) / bitrate
      //       = 8000f * chunk.length / bitrate - duration
      val sleep = math.round(8000f * chunk.length / (bitrate * realityFactor) - duration)
      implicit def f2r(f: () => Unit) = new Runnable { def run() { f() } }
      Server.executor.schedule(() => self ! NextChunk(chunks), sleep, TimeUnit.MILLISECONDS)
    } catch {
      case e: IOException =>
        log.info("Closing socket " + socket + " (" + e + ")")
        socket.close()
        self ! PoisonPill
    }
  }

  def generateMetadata(message: String): String = {
    val unPadded = "StreamTitle='%s';StreamUrl='';".format(message)
    unPadded + (" " * (16 - (unPadded.length % 16)))
  }

  def writeResponseHeaders(output: OutputStream) {
    val headers = "ICY 200 OK" ::
      "icy-notice1: Blahblah" ::
      "icy-name: scala shoutcast" ::
      "icy-url: http://localhost:" + Server.port ::
      "icy-metaint: %d".format(Server.chunkSize) ::
      "content-type: audio/mpeg" ::
      "\r\n" :: Nil

    output.write(headers.mkString("\r\n").getBytes("US-ASCII"))
  }
}
