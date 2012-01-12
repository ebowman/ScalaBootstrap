package com.gilt.hackathon

case class Ring[T](thing: Iterable[T]) extends Iterable[T] {
  require(thing.headOption.isDefined)

  def iterator = new Iterator[T] {
    private var iter = thing.iterator

    def hasNext = true

    def next() = {
      if (iter.hasNext) {
        iter.next()
      } else {
        iter = thing.iterator
        iter.next()
      }
    }
  }
}
