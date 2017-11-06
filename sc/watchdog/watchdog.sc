#!/usr/bin/env amm

import scalaj.http._
import ammonite.ops._
import fansi.Color._
import java.io.File
import $ivy.`org.slf4j:slf4j-nop:1.7.25`
import $ivy.`com.typesafe:config:1.3.1`
import com.typesafe.config.{Config, ConfigFactory}
import $ivy.`joda-time:joda-time:2.9.7`, org.joda.time.DateTime
import $ivy.`org.json4s::json4s-native:3.5.3`
import $ivy.`org.json4s::json4s-ext:3.5.3`
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
def refreshGithubInfo(): Unit = {
  saveInfo(getGithubInfo, gitPath, gitPathBak)
}

@main
def compare(): Unit = {
  val corClient = new CorClient(getConfig(corConfFile))
  val corInfo = loadInfo(corPath)
  val gitInfo = loadInfo(gitPath)
  traverse(corClient, corInfo, gitInfo)()
}

@main
def run(): Unit = {
  val corClient = new CorClient(getConfig(corConfFile))
  var corInfo = loadInfo(corPath)
  var gitInfo = loadInfo(gitPath)
  traverse(corClient, corInfo, gitInfo)(
    handleChanged = register(brandNew = false),
    handleAdded   = register(brandNew = true),
    handleRemoved = unregister
  )

  def register(brandNew: Boolean)(iri: String): Unit = {
    val sweetContents = Github.getSweet(iri)
    corClient.register(iri, sweetContents, brandNew)
    corInfo = corInfo.updated(iri, sha256(sweetContents))
    gitInfo = gitInfo.updated(iri, sha256(sweetContents))
  }

  def unregister(iri: String): Unit = {
    println("\t\t" + Red("unregistration not implemented."))
  }

  println("Updating local info...")
  saveInfo(corInfo, corPath, corPathBak)
  saveInfo(gitInfo, gitPath, gitPathBak)
}

def traverse(corClient: CorClient, corInfo: Map[String, Sha256], gitInfo: Map[String, Sha256])
            (handleChanged: String ⇒ Unit = (_) ⇒ (),
             handleAdded:   String ⇒ Unit = (_) ⇒ (),
             handleRemoved: String ⇒ Unit = (_) ⇒ ()
            ): Unit = {
  compareInfos(gitInfo, corInfo) match {
    case Some(CompareResult(changed, added, removed)) ⇒

      if (changed.nonEmpty) traverseChanged(changed)
      if (added.nonEmpty)   traverseAdded(added)
      if (removed.nonEmpty) traverseRemoved(removed)

      println(s"Done.")

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
  println("Getting Github info...")
  (Github.listPaths map { path ⇒
    val iri = "http://sweetontology.net/" + path.replaceFirst("\\.ttl$", "")
    println("\t" + iri)
    val contents = Github.getFile(path)
    iri → sha256(contents)
  }).toMap
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
