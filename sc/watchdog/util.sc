import fansi.Color._
import $ivy.`com.typesafe:config:1.3.1`
import com.typesafe.config.{Config, ConfigFactory}
import java.io.File

def getConfig(file: File): Config = {
  //println("reading " + corConfFile)
  val config = ConfigFactory.parseFile(file).resolve()
  if (!config.hasPath("userPass")) {
    error("userPass not defined in " + file)
  }
  else config
}

def error(str: String): Nothing = {
  Console.err.println(Red(s"error: $str"))
  System.exit(1)
  throw new RuntimeException
}
