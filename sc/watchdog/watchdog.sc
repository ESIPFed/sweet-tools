#!/usr/bin/env amm
//
// The main operation performed by this script is to check for any changed
// or new ontology files at Github and proceed with registering the updates
// at the COR. With user credentials and other information needed for the
// registrations provided in `cor.conf` in the current directory, run:
//
//   ./watchdog.sc run
//
// Detection of changes is based on relevant details retrieved from Github
// and compared with corresponding details previously stored locally in the
// file `details.csv`.
//
// Other commands also handled but mainly for inspection/debugging purposes:
//
//   ./watchdog.sc listUpdates
//    # lists all changed/new/removed files by comparing the
//    # remote file details against the locally stored details.
//
//   ./watchdog.sc listAll
//    # lists all ontology files at the SWEET Github repo.
//
//   ./watchdog.sc updateDetails
//    # Forces the update of the local `details.csv` file.
//
//   ./watchdog.sc listRegistered
//    # lists all SWEET ontologies registered at the COR.
//

import scalaj.http._
import ammonite.ops._
import fansi.Color._
import java.io.File
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

val corConfFile = new File("cor.conf")
val detailsPath: Path = pwd/"details.csv"
val detailsPathBak: Path = pwd/"details.bak.csv"

@main
def run(): Unit = {
  val corClient = new CorClient(getConfig(corConfFile))
  processAnyChanges()

  def processAnyChanges(): Unit = {
    getUpdates foreach { case (remoteDetails, cmp) ⇒
      if (cmp.changed.nonEmpty) processChanged(cmp.changed)
      if (cmp.added.nonEmpty)   processAdded(cmp.added)
      if (cmp.removed.nonEmpty) processRemoved(cmp.removed)

      println(s"\nUpdating local details")
      saveDetails(remoteDetails)

      println(s"Done.")
    }
  }

  def processChanged(paths: Seq[String]): Unit = {
    println(s"\nprocessing ${paths.size} changed files:")
    paths foreach { path ⇒
      println("\t" + Yellow(path))
      val (file, bytes) = Github.downloadFile(path)
      corClient.register(sweetIri(path), file, bytes, brandNew = false)
    }
  }

  def processAdded(paths: Seq[String]): Unit = {
    println(s"\nprocessing ${paths.size} added files:")
    paths foreach { path ⇒
      println("\t" + Yellow(path))
      val (file, bytes) = Github.downloadFile(path)
      corClient.register(sweetIri(path), file, bytes, brandNew = true)
    }
  }

  def processRemoved(paths: Seq[String]): Unit = {
    println(s"\nprocessing ${paths.size} removed files:")
    paths foreach { path ⇒
      println("\t" + Yellow(path))
      corClient.unregister(sweetIri(path))
    }
  }

  def sweetIri(path: String): String =
    "http://sweetontology.net/" + path.replaceFirst("\\.ttl$", "")
}


@main
def listUpdates(): Unit = {
  getUpdates.map(_._2) foreach { cmp ⇒
    doIt("Changed", cmp.changed)
    doIt("Added",   cmp.added)
    doIt("Removed", cmp.removed)
  }
  def doIt(what: String, paths: Seq[String]): Unit = {
    println(s"$what files:")
    if (paths.nonEmpty)
      paths.foreach(p ⇒ println("    %-30s".format(p)))
    else
      println("    (none)")
  }
}

@main
def listAll(): Unit = Github.listFiles foreach {i ⇒ println(i.path) }

@main
def updateDetails(): Unit = saveDetails(Github.listFiles)

@main
def listRegistered(): Unit = {
  val corClient = new CorClient(getConfig(corConfFile))
  corClient.listSweetOntologies.sortBy(_.uri) foreach { i ⇒
    println(i.uri)
  }
}

case class CompareResult(changed: Seq[String], added: Seq[String], removed: Seq[String])

def getUpdates: Option[(Seq[GithubFileInfo], CompareResult)] = {
  println("Retrieving remote info...")
  val remoteDetails = Github.listFiles
  println("Comparing to local info...")
  val remoteDetailsMap = remoteDetails.map(i ⇒ i.path → i).toMap
  val localDetailsMap = loadDetails
  val compareResultOpt = compare(remoteDetailsMap, localDetailsMap)
  compareResultOpt.map((remoteDetails, _)) orElse {
    println(Yellow("No changes."))
    None
  }
}

def compare(remoteDetailsMap: DetailsMap,
            localDetailsMap: DetailsMap
           ): Option[CompareResult] = {

  val remotePaths = remoteDetailsMap.keySet
  val localPaths  = localDetailsMap.keySet

  val commonPaths = remotePaths intersect localPaths
  val changed = commonPaths filter { commonPath ⇒
    val remoteDetail = remoteDetailsMap(commonPath)
    val localDetail = localDetailsMap(commonPath)
    remoteDetail != localDetail
  }
  val added = remotePaths.diff(localPaths).toSeq.sorted
  val removed = localPaths.diff(remotePaths).toSeq.sorted
  if (changed.nonEmpty || added.nonEmpty || removed.nonEmpty)
    Some(CompareResult(changed.toSeq.sorted, added, removed))
  else
    None
}

def loadDetails: DetailsMap = {
  val lines = read.lines(detailsPath).filterNot(_.startsWith("#"))
  val detailsMap: DetailsMap = (lines map { line ⇒
    val Array(path, sha, size) = line.split(",").map(_.trim)
    path → GithubFileInfo(path, sha, size.toInt)
  }).toMap
  detailsMap
}

def saveDetails(details: Seq[GithubFileInfo]): Unit = {
  val lines = details.sortBy(_.path) map { d ⇒
    "%-30s, %s, %s".format(d.path, d.sha, d.size)
  }
  cp.over(detailsPath, detailsPathBak)
  write.over(detailsPath, lines.mkString("\n"))
}
