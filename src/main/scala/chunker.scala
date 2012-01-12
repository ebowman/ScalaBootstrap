package com.gilt.hackathon

import java.io.OutputStream

object IcyResponder {
  def writeResponseHeaders(output: OutputStream) {
    val headers = "ICY 200 OK" ::
      "icy-notice1: Blahblah" ::
      "icy-name: scala shoutcast" ::
      "icy-url: http://localhost:" + Server.playlistPort ::
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
