//
// Github: Utility to retrieve SWEET info from Github.
//

import $ivy.`org.scalaj::scalaj-http:2.4.2`
import scalaj.http._
import ammonite.ops._
import java.io.File
import $ivy.`org.json4s::json4s-native:3.6.7`
import org.json4s._
import org.json4s.native.JsonMethods._
import $file.util, util._


case class GithubFileInfo(path: String, sha: String, size: Int)

object Github {
  implicit val jsonFormats = DefaultFormats

  def listPaths: Seq[String] = {
    List("src", "alignments")
      .flatMap(listPaths)
  }

  // the resulting paths will include the parentDir/ prefix
  private def listPaths(parentDir: String): Seq[String] = {
    val body = Http(s"https://api.github.com/repos/ESIPFed/sweet/contents/$parentDir").asString.body
    val items = parse(body).extract[Seq[GithubFileInfo]]
    items.filter(_.path.endsWith(".ttl")).map(_.path)
  }

  def getSweet(iri: String): String = {
    // TODO improve decision logic, which is pretty ad hoc at the moment.

    val ttlName = {
      if (iri == "http://sweetontology.net/alignment/ssn")
        "alignments/sweet-ssn-mapping.ttl"
      else if (iri == "http://sweetontology.net/alignment/dcat")
        "alignments/sweet-dcat-mapping.ttl"

      else
        "src/" + iri.substring("http://sweetontology.net/".length) + ".ttl"
    }
    getFile(ttlName)
  }

  def getSweetBytes(iri: String): Array[Byte] = {
    import java.nio.charset.StandardCharsets
    getSweet(iri).getBytes(StandardCharsets.UTF_8)
  }

  // ttlName assumed to include the parentDir,
  // eg., "src/..." or "alignments/..."
  def getFile(ttlName: String): String = {
    assert(ttlName.contains("/"), s"getFile: ttlName='$ttlName'")
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
