package com.gilt.hackathon

import java.io.{File, FileInputStream}

case class Chunker(curFile: File,
                   nextFiles: Iterator[File],
                   offset: Int,
                   chunkSize: Int) {
  def chunk: (Array[Byte], Chunker) = {
    val input = new FileInputStream(curFile)
    try {
      input.skip(offset)
      val buffer = new Array[Byte](chunkSize)
      val bytesRead = input.read(buffer)
      if (bytesRead < chunkSize) {
        // we rolled over
        val nextFile = nextFiles.next()
        val nextInput = new FileInputStream(nextFile)
        try {
          val nextBytesRead = nextInput.read(buffer, bytesRead, chunkSize - bytesRead)
          require(bytesRead + nextBytesRead == chunkSize, "chunk size spans more than 2 files")
          (buffer, Chunker(nextFile, nextFiles, nextBytesRead, chunkSize))
        } finally {
          nextInput.close()
        }
      } else {
        // we were able to read a full chunk
        require(bytesRead == chunkSize)
        (buffer, Chunker(curFile, nextFiles, offset + bytesRead, chunkSize))
      }
    } finally {
      input.close()
    }
  }
}
