//
// Github: Utility to retrieve info from Github.
//

import scalaj.http._
import ammonite.ops._
import java.io.File
import $ivy.`org.json4s::json4s-native:3.5.3`
import org.json4s._
import org.json4s.native.JsonMethods._
import $file.util, util._


type DetailsMap = Map[String, GithubFileInfo]

case class GithubFileInfo(path: String, sha: String, size: Int)

object Github {
  implicit val jsonFormats = DefaultFormats

  def listFiles: Seq[GithubFileInfo] = {
    val body = Http(s"https://api.github.com/repos/ESIPFed/sweet/contents").asString.body
    val items = parse(body).extract[Seq[GithubFileInfo]]
    items.filter(_.path.endsWith(".ttl"))
  }

  def downloadFile(ttlName: String): (File, Array[Byte]) = {
    val url = s"https://raw.githubusercontent.com/ESIPFed/sweet/master/$ttlName"
    println("\t\t- downloading " + url)
    val response: HttpResponse[String] = Http(url)
      .option(HttpOptions.followRedirects(true))
      .asString

    if (response.code != 200)
      error(s"failed to retrieve url=$url ⇒ ${response.code}: ${response.body}")

    val dir = new File("downloaded")
    dir.mkdir()
    val file = new File(dir, s"$ttlName")
    import java.nio.charset.StandardCharsets
    import java.nio.file.Files
    val bytes = response.body.getBytes(StandardCharsets.UTF_8)
    Files.write(file.toPath, bytes)
    //println("\t\t- download: wrote " + bytes.length + " to " + file)
    (file, bytes)
  }
}
