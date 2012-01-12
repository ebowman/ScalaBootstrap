package com.gilt.hackathon

import java.io._

object Server extends App {
  var counter = 0;
  val ring = new Ring(find(new File("."), _.getName.endsWith(".xml")))
  var chunker = new Chunker(ring, 64)
  while (counter < 500) {
    val myTuple = chunker.next
    chunker = myTuple._2
    val bytes = myTuple._1
    println(chunker.file.get.getName + ":" + chunker.position + "," + bytes.length + "," + new String(bytes))
    counter = counter + 1
  }
  
  def find(dir: File, filterFunc: (File => Boolean)): Iterable[File] = {
    require(dir.exists)
    require(dir.isDirectory)
    
    val inDir = dir.listFiles.filter(f => f.isFile && filterFunc(f))
    val inSubs = dir.listFiles.filter(_.isDirectory).flatMap(find(_, filterFunc))
    inDir ++ inSubs
  }
}

case class Chunker(filesIter: Iterator[File], chunkSize: Int, file: Option[File], position: Int) {
  
  def this(files: Iterable[File], chunkSize: Int) = this(files.iterator, chunkSize, Option.empty, 0)

  def next: (Array[Byte], Chunker) = {
    val currentFile = if (file.isDefined) file.get else filesIter.next

    val bytes = new Array[Byte](chunkSize)
    
    val is = new BufferedInputStream(new FileInputStream(currentFile))
    is.skip(position)
    val bytesRead = is.read(bytes, 0, chunkSize)
    
    if (bytesRead < chunkSize) {
      val myTuple = Chunker(filesIter, chunkSize - bytesRead, Option.empty, 0).next
      val missingBytes = myTuple._1
      val missingChunker = myTuple._2
      Array.copy(missingBytes, 0, bytes, bytesRead, chunkSize - bytesRead)
      (bytes, Chunker(filesIter, chunkSize, missingChunker.file, missingChunker.position))
    } else {
      (bytes, Chunker(filesIter, chunkSize, Option(currentFile), position + bytesRead))
    }
  }
  
}

class Ring[T](thing: Iterable[T]) extends Iterable[T] {
  
  private var innerIterator = thing.iterator
  
  def iterator = {
    new Iterator[T] {
      def hasNext = !thing.isEmpty
      
      def next = if (innerIterator.hasNext) {
        innerIterator.next
      } else {
        innerIterator = thing.iterator
        innerIterator.next
      }
    }
  }
}

