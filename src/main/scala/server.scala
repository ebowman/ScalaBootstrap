package com.gilt.hackathon

import java.io.File

object Server extends App {
  println(find(new File("."), _.getName.endsWith(".mp3")).toList.mkString("\n"))

  def find(dir: File, filterFunc: (File => Boolean)): Iterable[File] = {
    require(dir.exists && dir.isDirectory)
    def recurse(aDir: File): Iterable[File] = {
      val files = aDir.listFiles
      files.filter(_.isFile).filter(filterFunc) ++ files.filter(_.isDirectory).flatMap(recurse)
    }
    recurse(dir)
  }
}



