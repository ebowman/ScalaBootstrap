package com.gilt.hackathon

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import java.io.File

class RingSpec extends FlatSpec with ShouldMatchers {
  "A Ring" should "loop forever" in {
    val ring = new Ring("hello" :: "world" :: Nil)

    val iter = ring.iterator
    iter.next should equal ("hello")
    iter.next should equal ("world")
    iter.next should equal ("hello")
    iter.next should equal ("world")
  }

  it should "fail on an empty iterable" in {
    evaluating {
      new Ring(Nil)
    } should produce [IllegalArgumentException]
  }
}

class ChunkerSpec extends FlatSpec with ShouldMatchers {
  "A chunker" should "read 2 files forever" in {
    val files = new Ring(List("a.txt", "b.txt").map(f => new File("./src/test/resources/" + f)))
    val iterator = files.iterator
    val curFile = iterator.next()
    val chunker = Chunker(curFile, iterator, 0, 5)
    val (chunk1, chunker1) = chunker.chunk
    new String(chunk1) should equal("hello")
    val (chunk2, chunker2) = chunker1.chunk
    new String(chunk2) should  equal("\nworl")
    val (chunk3, chunker3) = chunker2.chunk
    new String(chunk3) should  equal("d\nhel")
    val (chunk4, _) = chunker3.chunk
    new String(chunk4) should  equal("lo\nwo")

  }
}
