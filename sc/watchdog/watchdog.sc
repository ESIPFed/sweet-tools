#!/usr/bin/env amm

import ammonite.ops._
import fansi.Color._
import $ivy.`org.scalaj::scalaj-http:2.4.2`
import scalaj.http._
import java.io.File
import $ivy.`org.slf4j:slf4j-nop:1.7.25`
import $ivy.`com.typesafe:config:1.3.1`
import com.typesafe.config.{Config, ConfigFactory}
import $ivy.`joda-time:joda-time:2.9.7`, org.joda.time.DateTime
import $ivy.`org.json4s::json4s-native:3.6.7`
import $ivy.`org.json4s::json4s-ext:3.6.7`
import org.json4s._
import org.json4s.ext.JodaTimeSerializers
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.writePretty
import $file.util, util._
import $file.github, github._
import $file.CorClient, CorClient.CorClient

implicit val jsonFormats = DefaultFormats ++ JodaTimeSerializers.all

val corConfFile = new File("local.cor.conf")

val corPath:    Path = pwd/"cor.csv"
val corPathBak: Path = pwd/"cor.bak.csv"

val gitPath:    Path = pwd/"github.csv"
val gitPathBak: Path = pwd/"github.bak.csv"

type Sha256 = String

case class CompareResult(changed: Seq[String], added: Seq[String], removed: Seq[String])


@main
def refreshCorInfo(): Unit = {
  val corClient = new CorClient(getConfig(corConfFile))
  saveInfo(getCorInfo(corClient), corPath, corPathBak)
}

@main
def compare(): Unit = {
  val gitInfo = refreshGithubInfo()
  val corClient = new CorClient(getConfig(corConfFile))
  val corInfo = loadInfo(corPath)
  traverse(corClient, corInfo, gitInfo)()
}

@main
def update(): Unit = {
  println("watchdog update starting " + new java.util.Date())
  var gitInfo = refreshGithubInfo()
  val corClient = new CorClient(getConfig(corConfFile))
  var corInfo = loadInfo(corPath)
  traverse(corClient, corInfo, gitInfo)(
    handleChanged = register(brandNew = false),
    handleAdded   = register(brandNew = true),
    handleRemoved = unregister,
    traverseEnded = updateInfo
  )

  def register(brandNew: Boolean)(iri: String): Unit = {
    val sweetContents = Github.getSweet(iri)
    try corClient.register(iri, sweetContents, brandNew)
    catch {
      case scala.util.control.NonFatal(e) ⇒
        println(s"Exception in register: brandNew=$brandNew  iri=$iri")
        e.printStackTrace()
        // but still let the updates below be performed
        println(s"NOTE: continuing...")
    }
    corInfo = corInfo.updated(iri, sha256(sweetContents))
    gitInfo = gitInfo.updated(iri, sha256(sweetContents))
  }

  def unregister(iri: String): Unit = {
    println("\t\t" + Red("unregistration not implemented."))
  }

  def updateInfo(cr: CompareResult): Unit = {
    print("Updating local info... ")
    Console.out.flush()
    saveInfo(corInfo, corPath, corPathBak)
    saveInfo(gitInfo, gitPath, gitPathBak)
    println("Done.")
  }
}

def refreshGithubInfo(): Map[String, Sha256] = {
  saveInfo(getGithubInfo, gitPath, gitPathBak)
}

def traverse(corClient: CorClient, corInfo: Map[String, Sha256], gitInfo: Map[String, Sha256])
            (handleChanged: String ⇒ Unit = (_) ⇒ (),
             handleAdded:   String ⇒ Unit = (_) ⇒ (),
             handleRemoved: String ⇒ Unit = (_) ⇒ (),
             traverseEnded: CompareResult ⇒ Unit = (_) ⇒ println(s"Done.")
            ): Unit = {
  compareInfos(gitInfo, corInfo) match {
    case Some(cr@CompareResult(changed, added, removed)) ⇒

      if (changed.nonEmpty) traverseChanged(changed)
      if (added.nonEmpty)   traverseAdded(added)
      if (removed.nonEmpty) traverseRemoved(removed)

      traverseEnded(cr)

    case None ⇒
      println(Yellow("No changes."))
  }

  def traverseChanged(iris: Seq[String]): Unit = {
    println(s"\n${iris.size} changed ontologies:")
    iris foreach { iri ⇒
      println("\t" + Yellow(iri))
      handleChanged(iri)
    }
  }

  def traverseAdded(iris: Seq[String]): Unit = {
    println(s"\n${iris.size} added ontologies:")
    iris foreach { iri ⇒
      println("\t" + Yellow(iri))
      handleAdded(iri)
    }
  }

  def traverseRemoved(iris: Seq[String]): Unit = {
    println(s"\n${iris.size} removed ontologies:")
    iris foreach { iri ⇒
      println("\t" + Yellow(iri))
      handleRemoved(iri)
    }
  }
}

def getCorInfo(corClient: CorClient): Map[String, Sha256] = {
  println("Getting COR info...")
  (corClient.listSweetOntologies.sortBy(_.uri) map { info ⇒
    println("\t" + info.uri)
    val contents = corClient.getOntology(info.uri)
    info.uri → sha256(contents)
  }).toMap
}

def getGithubInfo: Map[String, Sha256] = {
  print("Getting Github info: ")
  Console.out.flush()
  val elements = Github.listPaths map { path ⇒
    val iri: String = {
      if (path.startsWith("src/"))
        s"http://sweetontology.net/" + path.substring("src/".length)

      // TODO review/complement this as needed:
      else if (path == "alignments/sweet-ssn-mapping.ttl")
        "http://sweetontology.net/alignment/ssn"
      else if (path == "alignments/sweet-dcat-mapping.ttl")
        "http://sweetontology.net/alignment/dcat"

      else
        throw new RuntimeException(s"unexpected path: '$path'")
    }.replaceFirst("\\.ttl$", "")

    print(".")
    Console.out.flush()
    val contents = Github.getFile(path)
    iri → sha256(contents)
  }
  println(s"\n(${elements.length} paths retrieved)")
  elements.toMap
}

def saveInfo(info: Map[String, Sha256], path: Path, bak: Path): Map[String, Sha256] = {
  val lines = info.toSeq.sortBy(_._1) map { case (iri, sha256) ⇒
    "%-51s, %20s".format(iri, sha256)
  }
  val contents = "#%-50s, %64s\n%s".format("IRI", "sha256", lines.mkString("\n"))
  if (exists(path)) cp.over(path, bak)
  write.over(path, contents)
  info
}

def loadInfo(path: Path): Map[String, Sha256] = {
  val lines = read.lines(path).filterNot(_.startsWith("#"))
  (lines map { line ⇒
    val Array(iri, sha256) = line.split("\\s*,\\s*")
    iri → sha256
  }).toMap
}

def compareInfos(gitInfo: Map[String, Sha256],
                 corInfo: Map[String, Sha256]
                ): Option[CompareResult] = {

  val gitIris = gitInfo.keySet
  val corIris  = corInfo.keySet
  val common = gitIris intersect corIris

  val changed = common.filter(iri ⇒ corInfo(iri) != gitInfo(iri)).toSeq.sorted
  val added   = gitIris.diff(corIris).toSeq.sorted
  val removed = corIris.diff(gitIris).toSeq.sorted

  if (changed.nonEmpty || added.nonEmpty || removed.nonEmpty)
    Some(CompareResult(changed, added, removed))
  else
    None
}
