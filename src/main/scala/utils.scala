package com.gilt.hackathon

import java.io.{BufferedReader, InputStreamReader, InputStream}

class LineIterator(input: InputStream) extends Iterable[String] {
  private val reader = new BufferedReader(new InputStreamReader(input, "US-ASCII"))

  def iterator = new Iterator[String] {
    private var _next: String = _

    def hasNext = {
      _next = reader.readLine()
      _next != null
    }

    def next() = _next
  }
}

object ImplicitHelpers {
  implicit def f2r(f: () => Unit) = new Runnable {
    def run() {
      f()
    }
  }
}
