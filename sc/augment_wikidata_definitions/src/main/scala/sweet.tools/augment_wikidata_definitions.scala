package sweet.tools

import java.io.File
import java.io.FileWriter

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFFormat
import org.apache.jena.rdf.model._
import org.apache.jena.vocabulary._

import scala.collection.mutable.ArrayBuffer

// From Jena vocabulary package:
//   RDF.`type`  --> http://www.w3.org/1999/02/22-rdf-syntax-ns#type
//   OWL.Class   --> http://www.w3.org/2002/07/owl#Class
//   RDFS.label  --> http://www.w3.org/2000/01/rdf-schema#label

object augment_wikidata_definitions {

  def main(args: Array[String]): Unit = {
    if (args.nonEmpty) {
      main(args.head)
    }
    else println("Missing directory argument")
  }

  def main(dir: String): Unit = {
    val dirFile = new File(dir)

    println(s"listing .ttl files under ${dirFile.getCanonicalPath}")

    val files = dirFile
      .listFiles()
      .filter(_.getName.endsWith(".ttl"))
      .sortBy(_.getName)

    files foreach { file =>
      println(s"  loading ${file.getCanonicalPath}")
      loadModel(file) foreach { model =>
        println(s"   getting class resources")
        val owlClasses = model.listResourcesWithProperty(RDF.`type`, OWL.Class)
        if (owlClasses.hasNext) {
          while (owlClasses.hasNext) {
            val classResource = owlClasses.nextResource()
            println(s"  class resource: ${classResource}")
            val labelStatement = classResource.getProperty(RDFS.label, "en")
            println(s"  labelStatement: ${labelStatement}")
            if (labelStatement != null) {
              val label = getValueAsString(labelStatement.getObject)
              val wikidataDescription = executeWikidataDescriptionQuery(label)
              if (!Option(wikidataDescription).getOrElse("").isEmpty) {
                model.setNsPrefix("schema","http://schema.org/");  
                println(s"  ${model.expandPrefix("schema:description")}    ${wikidataDescription}")
                classResource.addLiteral(ResourceFactory.createProperty(
                        model.expandPrefix("schema:description")), wikidataDescription)
              }
            }
          }
        }
        else println("No class resources.")
        val outFile = new FileWriter(file)
        RDFDataMgr.write(outFile, model, org.apache.jena.riot.RDFFormat.TURTLE_PRETTY)
      }
    }

    def loadModel(file: File): Option[Model] = {
      try Some(RDFDataMgr.loadModel(file.getPath))
      catch {
        case e: Exception =>
          println(s"ERROR: $file: ${e.getMessage}")
          None
      }
    }

    def executeWikidataDescriptionQuery(label: String): String = {
      val query = getWikidataDescriptionQuery(label)
      val response = tryWith(QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", query)){qexec =>
        val results = qexec.execSelect()
        if (results.hasNext()) {
          var soln = results.next()
          return soln.getLiteral("o").getString()
        }
      }
      return null
    }

    def getWikidataDescriptionQuery(label: String): String = {
      s"""PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |PREFIX schema: <http://schema.org/>
         |SELECT ?o WHERE {
         |    ?s rdfs:label "${label}"@en .
         |    ?s schema:description ?o .
         |    FILTER(LANGMATCHES(LANG(?o), "en"))
         |    FILTER(STRLEN(?o) > 15)
         |}
         |""".stripMargin
    }

    def getValueAsString(node: RDFNode): String = node match {
      case lit: Literal  => lit.getLexicalForm
      case res: Resource => res.getURI
    }

    def tryWith[R, T <: AutoCloseable](resource: T)(doWork: T => R): R = {
      try {
        
        doWork(resource)
      }
      finally {
        try {
          if (resource != null) {
            resource.close()
          }
        }
        catch {
          case e: Exception => throw e
        }
      }
    }

  }

}

