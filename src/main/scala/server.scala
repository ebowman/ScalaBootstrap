package com.gilt.hackathon
import java.io.File

object Server extends App {
  val fileList = find(new File("/Users/kevinorirl/Music"), _.getName().endsWith(".mp3"))
  println(fileList.toList.mkString("\n"))
  
  def find(dir:File, filterFunc:(File=>Boolean)):Iterable[File] = {
    require(dir.exists() && dir.isDirectory())
    def recurse(aDir : File):Iterable[File] = {
    val inDir = dir.listFiles().filter(file => file.isFile() && filterFunc(file))
    val subDirs = dir.listFiles().filter(_.isDirectory())
    val subs = subDirs.flatMap(recurse)
    inDir++subs
  }
    recurse(dir)
  }
  

}

