package com.gilt.hackathon

import java.io.{File, FileInputStream, OutputStream}
import java.text.DecimalFormat
import javazoom.jl.decoder.Bitstream


case class ChunkIterator(chunkSize: Int, files: Iterable[File]) extends Iterator[Array[Byte]] {

  private var offset = 0
  private var iterator = files.iterator
  private var curFile = iterator.next()

  def hasNext = true

  def next(): Array[Byte] = {
    val buffer = new Array[Byte](chunkSize)
    var bytesRead = 0
    while (bytesRead < chunkSize) {
      val input = new FileInputStream(curFile)
      try {
        input.skip(offset)
        val thisBytesRead = input.read(buffer, bytesRead, chunkSize - bytesRead)
        if (thisBytesRead < chunkSize - bytesRead) {
          if (thisBytesRead > 0) {
            bytesRead += thisBytesRead
          }
          offset = 0
          curFile = if (iterator.hasNext) {
            iterator.next()
          } else {
            iterator = files.iterator; iterator.next()
          }
        } else {
          offset += thisBytesRead
          bytesRead += thisBytesRead
        }
      } finally {
        input.close()
      }
    }
    buffer
  }


  def curStatus: String = {
    curFile.getName + " " + new DecimalFormat("(##.#%)").format(1d * offset / curFile.length)
  }

  def curBitRate: Int = {
    val input = new FileInputStream(curFile)
    try {
      val bitstream = new Bitstream(input)
      val header = bitstream.readFrame
      bitstream.close()
      header.bitrate
    } finally {
      input.close()
    }
  }

}

object IcyResponder {
  def writeResponseHeaders(output: OutputStream, message: String, port: Int) {
    val headers = "ICY 200 OK" ::
      "icy-notice1: Blahblah" ::
      "icy-name: " + message ::
      "icy-url: http://localhost:" + port ::
      "icy-metaint: %d".format(Server.chunkSize) ::
      "content-type: audio/mpeg" ::
      "\r\n" :: Nil

    output.write(headers.mkString("\r\n").getBytes("US-ASCII"))
  }
}

class IcyResponder(chunks: ChunkIterator) {

  def writeNextChunk(output: OutputStream): Int = {
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
    math.round(8000f * chunk.length / (bitrate * realityFactor) - duration)
  }

  private def generateMetadata(message: String): String = {
    val unPadded = "StreamTitle='%s';StreamUrl='';".format(message)
    unPadded + (" " * (16 - (unPadded.length % 16)))
  }
}
