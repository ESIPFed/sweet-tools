#!/usr/bin/env amm
//
// This script reads in *.ttl files from a given directory and
// compares each file found there with the corresponding file
// in one other given directory using Jena's isIsomorphic check.
//
// USAGE:
//       ./check_isomorphic.sc dirA dirB [--reportAll]
//
// By default, only reports files that are not isomorphic or
// that have some loading error. Use --reportAll to report all.
//

import $ivy.`org.slf4j:slf4j-nop:1.7.25`
import $ivy.`org.apache.jena:jena:3.2.0`
import $ivy.`org.apache.jena:jena-tdb:3.2.0`
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.rdf.model.Model

import java.io.File

@main
def main(dirA: String, dirB: String, reportAll: Boolean = false) {
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
      println(s"$fileA: not found under $dirB")
    }
  }
  println(s"$okCount isomorphic files out of ${files.length}")

  def compare(fileA: File, fileB: File): Boolean = {
    if (reportAll)  {
      print(s"- ${fileA.getName}")
      Console.out.flush()
    }

    val result = for {
      modelA ← loadModel(fileA)
      modelB ← loadModel(fileB)
    } yield {
      val isomorphic = modelA.isIsomorphicWith(modelB)
      if (reportAll) {
        println(if (isomorphic) " √" else " Not isomorphic\n")
      }
      else if (!isomorphic) {
        println(s"- ${fileA.getName} NOT ISOMORPHIC\n")
      }
      isomorphic
    }

    result.getOrElse(false)
  }

  def loadModel(file: File): Option[Model] = {
    try Some(RDFDataMgr.loadModel(file.getPath))
    catch {
      case e: Exception ⇒
        if (!reportAll)  {
          print(s"- ${file.getName}")
        }
        println()
        val parentName = file.getParentFile.getName
        val name = parentName + "/" + file.getName
        println(s"  ERROR: $name: ${e.getMessage}\n")
        None
    }
  }
}
