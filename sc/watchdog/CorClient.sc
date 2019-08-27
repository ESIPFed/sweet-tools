//
// CorClient: Utility to interact with the COR API endpoint.
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
import $file.^.jena, jena.jena

implicit val jsonFormats = DefaultFormats ++ JodaTimeSerializers.all

case class OntologyInfo(
                         uri:          String,
                         version:      Option[String],
                         name:         Option[String],
                         ownerName:    Option[String],
                         author:       Option[String],
                         status:       Option[String],
                         ontologyType: Option[String],
                         format:       Option[String],
                         visibility:   Option[String]
                       )

case class UploadedFileInfo(userName: String,
                            filename: String,
                            format: String,
                            possibleOntologyUris: Map[String, PossibleOntologyInfo]
                           )

case class PossibleOntologyInfo(explanations: List[String],
                                metadata: Map[String,List[String]]
                               )

case class OntologyRegistrationResult(
                                       uri:         String,
                                       version:     Option[String] = None,
                                       visibility:  Option[String] = None,
                                       status:      Option[String] = None,
                                       registered:  Option[DateTime] = None,
                                       updated:     Option[DateTime] = None,
                                       removed:     Option[DateTime] = None
                                     )

class CorClient(config: Config) {
  val endpoint   = config.getString("endpoint")
  val userName   = config.getString("userName")
  val userPass   = config.getString("userPass")
  val orgName    = config.getString("orgName")
  val visibility = config.getString("visibility")
  val status     = config.getString("status")

  def listSweetOntologies: Seq[OntologyInfo] = {
    val url = endpoint + "/v0/ont"
    val response: HttpResponse[String] = Http(url)
      .option(HttpOptions.followRedirects(true))
      .asString

    if (response.code != 200)
      error(s"failed to retrieve url=$url ⇒ ${response.code}: ${response.body}")

    val items = parse(response.body).extract[Seq[OntologyInfo]]
    items.filter(_.uri.startsWith("http://sweetontology.net/"))
  }

  def getOntology(iri: String): String = {
    val url = endpoint + "/v0/ont"
    val response: HttpResponse[String] = Http(url)
      .param("oiri", iri)
      .option(HttpOptions.followRedirects(true))
      .header("User-Agent", "WatchdogCorClient/1")
      .asString

    if (response.code == 200)
      response.body
    else
      error(s"failed to retrieve url=$url ⇒ ${response.code}: ${response.body}")
  }

  def register(iri: String, sweetContents: String, brandNew: Boolean): Unit = {
    val uploadResult = uploadOntology(iri, sweetContents)
    //println(s"\t\t- uploadResult: filename=${uploadResult.filename} format=${uploadResult.format}")

    val name: String = {
      val ontModel = jena.loadOntModel(sweetContents, iri)
      val nameOpt = for {
        ontology ← Option(ontModel.getOntology(iri))
        name ← jena.getValue(ontology, org.apache.jena.vocabulary.RDFS.label)
      } yield name
      nameOpt getOrElse "-"
    }

    doRegister(iri, name, uploadResult, brandNew)
  }

  def uploadOntology(iri: String, sweetContents: String): UploadedFileInfo = {
    val route = endpoint + "/v0/ont/upload"
    println("\t\t- uploading")

    import java.nio.charset.StandardCharsets
    val bytes = sweetContents.getBytes(StandardCharsets.UTF_8)

    val response: HttpResponse[String] = Http(route)
      .header("User-Agent", "WatchdogCorClient/1")
      .timeout(connTimeoutMs = 5*1000, readTimeoutMs = 3*60*1000)
      .postMulti(MultiPart("file", "filename", "text/plain", bytes))
      .param("format", "ttl")
      .auth(userName, userPass)
      .asString

    if (response.code != 200) {
      error(s"uploading iri=$iri: response=$response")
    }

    parse(response.body).extract[UploadedFileInfo]
  }

  def doRegister(iri: String, name: String, ufi: UploadedFileInfo, brandNew: Boolean
                ): OntologyRegistrationResult = {

    val params = List[(String, String)](
      "iri" → iri,
      "format" → "ttl",
      "name" → name,
      "userName" → userName,
      "orgName" → orgName,
      "visibility" → visibility,
      "status" → status,
      "uploadedFilename" → ufi.filename,
      "uploadedFormat" → ufi.format
    )

    val route = endpoint + "/v0/ont"

    if (brandNew) {
      println(s"\t\t- registering brand new iri=$iri")
      val response: HttpResponse[String] = Http(route)
        .header("User-Agent", "WatchdogCorClient/1")
        .timeout(connTimeoutMs = 5*1000, readTimeoutMs = 3*60*1000)
        .postForm(params)
        .auth(userName, userPass)
        .asString

      //println(s"response: code=%s body=%s" format (response.code, Yellow(response.body)))

      if (response.code == 201) {
        val res = parse(response.body).extract[OntologyRegistrationResult]
        println("\t\t  => " + Green("OK") + res.version.map(v ⇒ s" ($v)").getOrElse(""))
        res
      }
      else
        error(s"registering brand new entry for iri=$iri: " + response)
    }
    else {
      println(s"\t\t- registering new revision of iri=$iri")
      val data = writePretty(params.toMap)
      val response: HttpResponse[String] = Http(route)
        .header("User-Agent", "WatchdogCorClient/1")
        .timeout(connTimeoutMs = 5*1000, readTimeoutMs = 3*60*1000)
        .auth(userName, userPass)
        .postData(data)
        .header("Content-type", "application/json")
        .method("PUT")
        .asString

      //println(s"response: code=%s body=%s" format (response.code, Yellow(response.body)))

      if (response.code == 200) {
        val res = parse(response.body).extract[OntologyRegistrationResult]
        println("\t\t  => " + Green("OK") + res.version.map(v ⇒ s" ($v)").getOrElse(""))
        res
      }
      else
        error(s"registering new revision of iri=$iri: " + response)
    }
  }
}
