scalaVersion     := "2.13.1"
version          := "0.1.0-SNAPSHOT"
organization     := "org.esipfed"
organizationName := "sweet"

lazy val root = (project in file("."))
  .settings(
    name := "augment_wikidata_definitions",

    libraryDependencies ++= Seq(
      ("org.apache.jena" % "jena-core" % "3.15.0").exclude("org.slf4j", "slf4j-log4j12"),
      "org.apache.jena" % "jena-tdb" % "3.15.0",
      "net.sourceforge.owlapi" % "owlapi-distribution" % "5.1.15",
      "org.slf4j" % "slf4j-nop" % "1.7.25",
      "com.github.tototoshi" % "scala-csv_2.13" % "1.3.6"
    )
  )

