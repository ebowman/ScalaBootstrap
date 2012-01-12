package com.gilt.hackathon

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import java.io.File

class ChunkIteratorSpec extends FlatSpec with ShouldMatchers {
  val files = List("a.txt", "b.txt").map(f => new File("./src/test/resources/" + f))

  "A ChunkIterator" should "read 2 files forever" in {
    val iter = new ChunkIterator(5, files)
    iter.take(5).map(new String(_)).toList should equal(List(
    "hello", "\nworl", "d\nhel", "lo\nwo", "rld\nh"
    ))
  }

  it should "work if the chunk size spans files" in {
    val iter = new ChunkIterator(10, files)
    iter.take(5).map(new String(_)).toList should equal(List(
    "hello\nworl", "d\nhello\nwo", "rld\nhello\n", "world\nhell", "o\nworld\nhe"
    ))

  }
}
