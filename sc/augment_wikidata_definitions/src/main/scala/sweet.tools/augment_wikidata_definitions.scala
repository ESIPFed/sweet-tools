package sweet.tools

import java.io.{File, FileOutputStream}
import java.util
import java.util.Optional

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.rdf.model._
import org.semanticweb.owlapi.formats
import org.semanticweb.owlapi.model._
import org.semanticweb.owlapi.search.EntitySearcher

object augment_wikidata_definitions {

  def main(args: Array[String]): Unit = {
    if (args.nonEmpty) {
      main(args.head)
    }
    else println("Missing sweet directory argument")
  }

  def main(inDir: String): Unit = {
    val dirFile = new File(inDir)

    println(s"listing .ttl files under ${dirFile.getCanonicalPath}")

    val files = dirFile
      .listFiles()
      .filter(_.getName.endsWith(".ttl"))
      .sortBy(_.getName)

    files foreach { file =>
      println(s"  loading ${file.getCanonicalPath}")
      import org.semanticweb.owlapi.apibinding.OWLManager
      val manager = OWLManager.createOWLOntologyManager
      loadModel(file, manager) foreach { owlOntology =>
        val changes = new util.ArrayList[OWLOntologyChange]()
        val df = manager.getOWLDataFactory
        val oldVersionAnnotation = df.getOWLAnnotation(df.getOWLVersionInfo, df.getOWLLiteral("3.5.0"))
        changes.add(new RemoveOntologyAnnotation(owlOntology, oldVersionAnnotation))
        val newVersionAnnotation = df.getOWLAnnotation(df.getOWLVersionInfo, df.getOWLLiteral("3.6.0"))
        changes.add(new AddOntologyAnnotation(owlOntology, newVersionAnnotation))
        import org.semanticweb.owlapi.model.IRI
        import org.semanticweb.owlapi.model.OWLOntologyID
        import org.semanticweb.owlapi.model.SetOntologyID
        val versionIRI = IRI.create(owlOntology.getOntologyID.getOntologyIRI + "/3.6.0")
        val newVersionIRI = new SetOntologyID(owlOntology,
          new OWLOntologyID(owlOntology.getOntologyID.getOntologyIRI, Optional.of(versionIRI)))
        changes.add(newVersionIRI)
        println(s"   getting class resources")
        val classIter = owlOntology.classesInSignature().iterator()
        while (classIter.hasNext) {
          val owlClass = classIter.next()
          println(s"  class resource: ${owlClass.toString}")
          val annotation = EntitySearcher.getAnnotations(owlClass, owlOntology, df.getRDFSLabel()).findFirst()
          if (annotation.isPresent) {
            val annotString = annotation.get().annotationValue().toString
            val trimmedLabel = annotString.substring(1, annotString.length - 4)
            println(s"    label statement: ${trimmedLabel}")
            val wikidataDescription = executeWikidataDescriptionQuery(trimmedLabel)
            if (wikidataDescription != null) {
              println(s"      rdfs:comment: ${wikidataDescription.getLexicalForm}")
              val commentAnno = df.getOWLAnnotation(
                df.getRDFSComment,
                df.getOWLLiteral(wikidataDescription.getLexicalForm, "en"))
              val axiom = df.getOWLAnnotationAssertionAxiom(owlClass.getIRI(), commentAnno)
              changes.add(new AddAxiom(owlOntology, axiom))
            }
          }
        };
        manager.applyChanges(changes)
        val fos = new FileOutputStream(file)
        manager.saveOntology(owlOntology, new formats.TurtleDocumentFormat(), fos)
        fos.close()
        manager.clearOntologies()
        manager.removeOntology(owlOntology)
//        val owlManager = OWLManager.createOWLOntologyManager
//        val ont = owlManager.loadOntologyFromOntologyDocument(file)
//        import java.io.File
//        import org.semanticweb.owlapi.model.IRI
//        val output = File.createTempFile("saved_file", "ttl")
//        val documentIRI2 = IRI.create(output)
//        // save in Turtle format
//        owlManager.saveOntology(owlOntology, new formats.TurtleDocumentFormat(), documentIRI2);
//        // Remove the ontology from the manager
//        owlManager.removeOntology(ont);

      }
    }

    def loadModel(file: File, manager: OWLOntologyManager): Option[OWLOntology] = {
      try Some(manager.loadOntologyFromOntologyDocument(file.getAbsoluteFile))
      catch {
        case e: Exception =>
          println(s"ERROR: $file: ${e.getMessage}")
          None
      }
    }

    def executeWikidataDescriptionQuery(trimmedLabel: String): Literal = {
      val query = getWikidataDescriptionQuery(trimmedLabel)
      val response: Unit = tryWith(QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", query)){ qexec =>
        val results = qexec.execSelect()
        if (results.hasNext) {
          val soln = results.next
          return soln.getLiteral("o")
        }
      }
      return null
    }

    def getWikidataDescriptionQuery(trimmedLabel: String): String = {
      s"""PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |PREFIX schema: <http://schema.org/>
         |SELECT ?o WHERE {
         |    ?s rdfs:label "$trimmedLabel"@en .
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

