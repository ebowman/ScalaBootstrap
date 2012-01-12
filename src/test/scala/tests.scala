package com.gilt.hackathon

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec

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
