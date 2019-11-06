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

import java.io.{File, ByteArrayOutputStream}

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
      if (isomorphic) {
        print(" √")
      }
      else {
        val reportName = s"${fileA.getName}.diff.txt"
        print(s" NOT ISOMORPHIC, see $reportName")
        reportDiff(reportName, modelA, modelB)
      }
      isomorphic
    }

    result.getOrElse(false)
  }

  def reportDiff(reportName: String, modelA: Model, modelB: Model): Unit = {
    val triplesA = modelToNTriples(modelA)
    val triplesB = modelToNTriples(modelB)

    val aDiffB = triplesA.diff(triplesB)
    val bDiffA = triplesB.diff(triplesA)

    val report =
      s"""
         |${aDiffB.size} triples in 1st model but not in the 2nd:
         |  ${aDiffB.toList.sorted.mkString("\n  ")}
         |
         |${bDiffA.size} triples in 2nd model but not in the 1st:
         |  ${bDiffA.toList.sorted.mkString("\n  ")}
         |""".stripMargin

    import ammonite.ops.{write, pwd}
    write.over(pwd / reportName, report)
  }

  def modelToNTriples(model: Model): Set[String] = {
    val ba = new ByteArrayOutputStream()
    model.write(ba, "n-triples")
    val str = ba.toString()
    str.split("\n").toSet
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
