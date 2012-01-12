package com.gilt.hackathon

import java.io.{FileInputStream, File}


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
}
