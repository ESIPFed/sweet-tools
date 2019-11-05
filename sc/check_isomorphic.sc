#!/usr/bin/env amm
//
// This script reads in *.ttl files from a given directory and
// compares each file found there with the corresponding file
// in one other given directory using Jena's isIsomorphic check.
//
// USAGE:
//       ./check_isomorphic.sc dirA dirB
//

import $ivy.`org.slf4j:slf4j-nop:1.7.25`
import $ivy.`org.apache.jena:jena:3.2.0`
import $ivy.`org.apache.jena:jena-tdb:3.2.0`
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.rdf.model.Model

import java.io.File

@main
def main(dirA: String, dirB: String) {
  val dirAFile = new File(dirA)

  val files = dirAFile
    .listFiles()
    .filter(_.getName.endsWith(".ttl"))
    .sortBy(_.getName)

  var okCount = 0

  files foreach { fileA ⇒
    val fileB = new File(dirB, fileA.getName)
    if (fileB.exists()) {
      val ok = compare(fileA, fileB)
      if (ok) {
        okCount += 1
      }
    }
    else {
      println(s"$fileA: not found under $dirB\n")
    }
  }
  println(s"\n$okCount isomorphic files out of ${files.length}")

  def compare(fileA: File, fileB: File): Boolean = {
    print(s"\n- ${fileA.getName}")
    Console.out.flush()

    val modelAOpt = loadModel(fileA)
    val modelBOpt = loadModel(fileB)

    val result = for {
      modelA ← modelAOpt
      modelB ← modelBOpt
    } yield {
      val isomorphic = modelA.isIsomorphicWith(modelB)
      print(if (isomorphic) " √" else " NOT ISOMORPHIC")
      isomorphic
    }

    result.getOrElse(false)
  }

  def loadModel(file: File): Option[Model] = {
    try Some(RDFDataMgr.loadModel(file.getPath))
    catch {
      case e: Exception ⇒
        val parentName = file.getParentFile.getName
        val name = parentName + "/" + file.getName
        print(s"\n  ERROR: $name: ${e.getMessage}")
        None
    }
  }
}
