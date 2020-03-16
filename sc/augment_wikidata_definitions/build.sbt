scalaVersion     := "2.13.1"
version          := "0.1.0-SNAPSHOT"
organization     := "com.example"
organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "augment_wikidata_definitions",

    libraryDependencies ++= Seq(
      ("org.apache.jena" % "jena-core" % "3.14.0").exclude("org.slf4j", "slf4j-log4j12"),

      "org.apache.jena" % "jena-tdb" % "3.14.0",
      // Per https://jena.apache.org/download/maven.html:
      //  "...use of <type>pom</type> ... does not work in all tools.
      //  An alternative is to depend on jena-tdb, which will pull in the other artifacts."

      "org.slf4j" % "slf4j-nop" % "1.7.25",
    )
  )

