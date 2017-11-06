//
// Github: Utility to retrieve SWEET info from Github.
//

import scalaj.http._
import ammonite.ops._
import java.io.File
import $ivy.`org.json4s::json4s-native:3.5.3`
import org.json4s._
import org.json4s.native.JsonMethods._
import $file.util, util._


case class GithubFileInfo(path: String, sha: String, size: Int)

object Github {
  implicit val jsonFormats = DefaultFormats

  def listPaths: Seq[String] = listFiles.map(_.path)

  def listFiles: Seq[GithubFileInfo] = {
    val body = Http(s"https://api.github.com/repos/ESIPFed/sweet/contents").asString.body
    val items = parse(body).extract[Seq[GithubFileInfo]]
    items.filter(_.path.endsWith(".ttl"))
  }

  def getSweet(iri: String): String = {
    val ttlName = iri.substring("http://sweetontology.net/".length) + ".ttl"
    getFile(ttlName)
  }

  def getSweetBytes(iri: String): Array[Byte] = {
    import java.nio.charset.StandardCharsets
    getSweet(iri).getBytes(StandardCharsets.UTF_8)
  }

  def getFile(ttlName: String): String = {
    val url = s"https://raw.githubusercontent.com/ESIPFed/sweet/master/$ttlName"
    val response: HttpResponse[String] = Http(url)
      .option(HttpOptions.followRedirects(true))
      .asString

    if (response.code == 200)
      response.body
    else
      error(s"failed to retrieve url=$url ⇒ ${response.code}: ${response.body}")
  }
}
