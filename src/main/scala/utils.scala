package com.gilt.hackathon

import java.io.{BufferedReader, InputStreamReader, InputStream}

/**
 * Utility to iterate through header lines.
 */
case class LineIterator(input: InputStream) extends Iterator[String] {
  private val reader = new BufferedReader(new InputStreamReader(input, "US-ASCII"))
  private var nextLine: String = _

  def hasNext = {
    nextLine = reader.readLine()
    nextLine != null
  }

  def next(): String = {
    nextLine
  }
}
