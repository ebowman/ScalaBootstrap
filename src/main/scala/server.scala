package com.gilt.hackathon

import java.io._
import java.net._
import akka.actor.Actor

object Server extends App {

  val serverSocket = new ServerSocket(8080)
  while (true) {
    val socket = serverSocket.accept()
    val actor = Actor.actorOf(new RequestActor).start()
    actor ! StartStream(socket, new Ring(find(new File("/home/boniek/mp3"), _.getName.endsWith(".mp3"))))
  }
  
  def find(dir: File, filterFunc: (File => Boolean)): Iterable[File] = {
    require(dir.exists)
    require(dir.isDirectory)
    
    val inDir = dir.listFiles.filter(f => f.isFile && filterFunc(f))
    val inSubs = dir.listFiles.filter(_.isDirectory).flatMap(find(_, filterFunc))
    inDir ++ inSubs
  }
}

case class StartStream(socket: Socket, files: Iterable[File])
case class NextChunk(output: OutputStream, socket: Socket, chunker: Chunker)

class RequestActor() extends Actor {
  val chunkSize = 1024
  
  def receive = {
    case StartStream(socket, files) =>
      val output = socket.getOutputStream
      writeResponseHeaders(output)
      self ! NextChunk(output, socket, new Chunker(files, chunkSize))
    case NextChunk(output, socket, chunker) =>
      val tuple = chunker.next
      val chunkBytes = tuple._1
      val newChunker = tuple._2
      output.write(chunkBytes)
      writeMetadata(output, newChunker.file.get)
      self ! NextChunk(output, socket, newChunker)
  }
  
  def writeMetadata(output: OutputStream, file: File) {
    val meta = "StreamTitle=" + file.getName + ";"
    val metaBytes = meta.getBytes("US-ASCII")
    val metaBlocks = metaBytes.length / 16 + 1
    val remainingBytes = Array.fill(metaBlocks * 16 - metaBytes.length) { 0.toByte }

    output.write({metaBlocks.toByte})
    output.write(metaBytes ++ remainingBytes)
  }
  
  def writeResponseHeaders(output: OutputStream) {
    val headers = "ICY 200 OK" ::
      "icy-notice1: Blahblah" ::
      "icy-name: scala shoutcast" ::
      "icy-url: http://localhost:8080" ::
      "icy-metaint: " + chunkSize ::
      "content-type: audio/mpeg" ::
      "\r\n" :: Nil

    output.write(headers.mkString("\r\n").getBytes("US-ASCII"))
  }
  
}

case class Chunker(filesIter: Iterator[File], chunkSize: Int, file: Option[File], position: Int) {
  
  def this(files: Iterable[File], chunkSize: Int) = this(files.iterator, chunkSize, Option.empty, 0)

  def next: (Array[Byte], Chunker) = {
    val currentFile = if (file.isDefined) file.get else filesIter.next

    val bytes = new Array[Byte](chunkSize)
    
    val is = new BufferedInputStream(new FileInputStream(currentFile))
    try {
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
    } finally {
      is.close();
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

