package com.gilt.hackathon

import java.net.Socket
import org.slf4j.LoggerFactory
import akka.actor.{PoisonPill, Actor}
import java.io.{IOException, OutputStream, File}
import java.util.concurrent.TimeUnit

case class StartStreaming(socket: Socket, files: Iterable[File])

case class NextChunk(output: OutputStream, socket: Socket, chunks: ChunkIterator)

object RequestActor {
  val log = LoggerFactory.getLogger(classOf[RequestActor])
}

class RequestActor extends Actor {
  import RequestActor.log

  def receive = {
    case StartStreaming(socket, files) =>
      try {
        // block & suck until blank line (end of headers)
        // (later we might like to look at these headers...)
        new LineIterator(socket.getInputStream).dropWhile(_ != "")
        val output = socket.getOutputStream
        writeResponseHeaders(output)
        self ! NextChunk(output, socket, new ChunkIterator(Server.chunkSize, files))
      }
    case NextChunk(output, socket, chunks) =>
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
        val realityFactor = 1.1f
        // effectiveBitsPerSecond = 8000f * chunk.length / duration
        // bitrate = 8000f * chunk.length / (duration + sleep)
        // bitrate*duration + bitrate*sleep = 8000f * chunk.length
        // bitrate * sleep = 8000f * chunk.length - bitrate * duration
        // sleep = (8000f * chunk.length - bitrate * duration) / bitrate
        //       = 8000f * chunk.length / bitrate - duration
        val sleep = math.round(8000f * chunk.length / (bitrate * bitrate) - duration)
        implicit def f2r(f: () => Unit) = new Runnable { def run() { f() } }
        Server.executor.schedule(() => self ! NextChunk(output, socket, chunks), sleep, TimeUnit.MILLISECONDS)
      } catch {
        case e: IOException =>
          log.info("Closing socket " + socket)
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
