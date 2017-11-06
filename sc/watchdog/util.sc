import fansi.Color._
import $ivy.`com.typesafe:config:1.3.1`
import com.typesafe.config.{Config, ConfigFactory}
import java.io.File
import java.security.MessageDigest


def getConfig(file: File): Config = {
  //println("reading " + corConfFile)
  val config = ConfigFactory.parseFile(file).resolve()
  if (!config.hasPath("userPass")) {
    error("userPass not defined in " + file)
  }
  else config
}

val msgDigest = MessageDigest.getInstance("SHA-256")

def sha256(str: String): String = {
  sha256(str.getBytes("UTF-8"))
}

def sha256(bytes: Array[Byte]): String = {
  msgDigest.digest(bytes).map("%02x".format(_)).mkString
}

def error(str: String): Nothing = {
  Console.err.println(Red(s"error: $str"))
  System.exit(1)
  throw new RuntimeException
}
