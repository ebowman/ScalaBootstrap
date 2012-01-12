package com.gilt.hackathon

import akka.actor.Actor
import java.net.Socket
import java.io.{OutputStream, File}
import org.slf4j.LoggerFactory

case class StartStreaming(socket: Socket, files: Iterable[File])

case class NextChunk(output: OutputStream, socket: Socket, chunks: Iterator[Array[Byte]])

object RequestActor {
  val log = LoggerFactory.getLogger(classOf[RequestActor])
}

class RequestActor extends Actor {
  def receive = {
    case StartStreaming(socket, files) =>
      try {
        val chunker = new ChunkIterator(Server.chunkSize, files)
        new LineIterator(socket.getInputStream).dropWhile(_ != "") // block & suck until blank line (end of headers)
        val output = socket.getOutputStream
        writeResponseHeaders(output)
        self ! NextChunk(output, socket, chunker)
      }
    case NextChunk(output, socket, chunks) =>

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
