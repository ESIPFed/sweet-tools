package sweet.tools

import java.io.{File, FileOutputStream}
import java.time.LocalDateTime
import java.util
import java.util.Optional

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.rdf.model._
import org.semanticweb.owlapi.formats
import org.semanticweb.owlapi.model._
import org.semanticweb.owlapi.search.EntitySearcher
import org.semanticweb.owlapi.vocab.OWL2Datatype

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
        import org.semanticweb.owlapi.model.{IRI, OWLOntologyID, SetOntologyID}
        val versionIRI = IRI.create(owlOntology.getOntologyID.getOntologyIRI.get() + "/3.6.0")
        val newVersionIRI = new SetOntologyID(owlOntology,
          new OWLOntologyID(
            owlOntology.getOntologyID.getOntologyIRI.get(), Optional.of(versionIRI).get()))
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
            val defProp = df.getOWLAnnotationProperty("http://www.w3.org/2004/02/skos/core#definition")
            if (wikidataDescription != null) {
              if (EntitySearcher.getAnnotationObjects(owlClass, owlOntology, defProp).count() == 0) {
                //skos:definition
                val skosAnno = df.getOWLAnnotation(defProp, df.getOWLAnonymousIndividual)
                val skosAxiom = df.getOWLAnnotationAssertionAxiom(owlClass.getIRI(), skosAnno)
                changes.add(new AddAxiom(owlOntology, skosAxiom))
                //rdfs:comment
                val commentAnno = df.getOWLAnnotation(
                  df.getRDFSComment,
                  df.getOWLLiteral(wikidataDescription.get(1), "en"))
                val commentAxiom = df.getOWLAnnotationAssertionAxiom(
                  df.getOWLAnnotationProperty("http://www.w3.org/2000/01/rdf-schema#comment"),
                  skosAxiom.anonymousIndividualValue().get(), commentAnno.annotationValue())
                changes.add(new AddAxiom(owlOntology, commentAxiom))
                //prov:wasDerivedFrom
                //              val wdfProp = df.getOWLAnnotationProperty("http://www.w3.org/ns/prov#wasDerivedFrom")
                //              val provAnno = df.getOWLAnnotation(wdfProp, IRI.create(wikidataDescription.get(0)))
                //              val provAxiom = df.getOWLAnnotationAssertionAxiom(
                //                wdfProp, skosAxiom.anonymousIndividualValue().get(), provAnno.annotationValue())
                //              changes.add(new AddAxiom(owlOntology, provAxiom))
                //dcterms:source
                val sProp = df.getOWLAnnotationProperty("http://purl.org/dc/terms/source")
                val sourceAnno = df.getOWLAnnotation(sProp, IRI.create(wikidataDescription.get(0)))
                val sourceAxiom = df.getOWLAnnotationAssertionAxiom(
                  sProp, skosAxiom.anonymousIndividualValue().get(), sourceAnno.annotationValue())
                changes.add(new AddAxiom(owlOntology, sourceAxiom))
                //dcterms:created
                val ldt = LocalDateTime.now();
                val cProp = df.getOWLAnnotationProperty("http://purl.org/dc/terms/created")
                val createdAnno = df.getOWLAnnotation(cProp, df.getOWLLiteral(ldt.toString, OWL2Datatype.XSD_DATE_TIME))
                val createdAxiom = df.getOWLAnnotationAssertionAxiom(
                  cProp, skosAxiom.anonymousIndividualValue().get(), createdAnno.annotationValue())
                changes.add(new AddAxiom(owlOntology, createdAxiom))
                //dcterms:creator
                val crProp = df.getOWLAnnotationProperty("http://purl.org/dc/terms/creator")
                val creatorAnno = df.getOWLAnnotation(crProp, IRI.create("https://orcid.org/0000-0003-2185-928X"))
                val creatorAxiom = df.getOWLAnnotationAssertionAxiom(
                  crProp, skosAxiom.anonymousIndividualValue().get(), creatorAnno.annotationValue())
                changes.add(new AddAxiom(owlOntology, creatorAxiom))
            } else {
            //skos:definition
            val skosAnno = df.getOWLAnnotation(defProp, df.getOWLAnonymousIndividual)
            val skosAxiom = df.getOWLAnnotationAssertionAxiom(owlClass.getIRI(), skosAnno)
            changes.add(new AddAxiom(owlOntology, skosAxiom))
            //skos:historyNote
            val historicalAnno = df.getOWLAnnotation(
              df.getOWLAnnotationProperty("http://www.w3.org/2004/02/skos/core#historyNote"),
              df.getOWLLiteral("Native curated definition by ESIP Semantic Harmonization Commitee.", "en"))
            //rdfs:comment
            val commentAnno = df.getOWLAnnotation(
              df.getRDFSComment,
              df.getOWLLiteral(wikidataDescription.get(1), "en"))
            val commentAxiom = df.getOWLAnnotationAssertionAxiom(
              df.getOWLAnnotationProperty("http://www.w3.org/2000/01/rdf-schema#comment"),
              skosAxiom.anonymousIndividualValue().get(), commentAnno.annotationValue())
            changes.add(new AddAxiom(owlOntology, commentAxiom))
            //prov:wasDerivedFrom
            //              val wdfProp = df.getOWLAnnotationProperty("http://www.w3.org/ns/prov#wasDerivedFrom")
            //              val provAnno = df.getOWLAnnotation(wdfProp, IRI.create(wikidataDescription.get(0)))
            //              val provAxiom = df.getOWLAnnotationAssertionAxiom(
            //                wdfProp, skosAxiom.anonymousIndividualValue().get(), provAnno.annotationValue())
            //              changes.add(new AddAxiom(owlOntology, provAxiom))
            //dcterms:source
            val sProp = df.getOWLAnnotationProperty("http://purl.org/dc/terms/source")
            val sourceAnno = df.getOWLAnnotation(sProp, IRI.create(wikidataDescription.get(0)))
            val sourceAxiom = df.getOWLAnnotationAssertionAxiom(
              sProp, skosAxiom.anonymousIndividualValue().get(), sourceAnno.annotationValue())
            changes.add(new AddAxiom(owlOntology, sourceAxiom))
            //dcterms:created
            val ldt = LocalDateTime.now();
            val cProp = df.getOWLAnnotationProperty("http://purl.org/dc/terms/created")
            val createdAnno = df.getOWLAnnotation(cProp, df.getOWLLiteral(ldt.toString, OWL2Datatype.XSD_DATE_TIME))
            val createdAxiom = df.getOWLAnnotationAssertionAxiom(
              cProp, skosAxiom.anonymousIndividualValue().get(), createdAnno.annotationValue())
            changes.add(new AddAxiom(owlOntology, createdAxiom))
            //dcterms:creator
            val crProp = df.getOWLAnnotationProperty("http://purl.org/dc/terms/creator")
            val creatorAnno = df.getOWLAnnotation(crProp, IRI.create("https://orcid.org/0000-0003-2185-928X"))
            val creatorAxiom = df.getOWLAnnotationAssertionAxiom(
              crProp, skosAxiom.anonymousIndividualValue().get(), creatorAnno.annotationValue())
            changes.add(new AddAxiom(owlOntology, creatorAxiom))
          }
          }}
        };
        manager.applyChanges(changes)
        val fos = new FileOutputStream(file)
        val format = new formats.TurtleDocumentFormat()
        format.setDefaultPrefix(owlOntology.getOntologyID.getOntologyIRI.get().getIRIString + "/")
        format.setPrefix("dcterms", "http://purl.org/dc/terms/")
        //format.setPrefix("prov", "http://www.w3.org/ns/prov#")
        format.setPrefix("skos", "http://www.w3.org/2004/02/skos/core#")
        manager.saveOntology(owlOntology, format, fos)
        fos.close()
        manager.clearOntologies()
        manager.removeOntology(owlOntology)
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

    def executeWikidataDescriptionQuery(trimmedLabel: String): util.ArrayList[String] = {
      val query = getWikidataDescriptionQuery(trimmedLabel)
      val response: Unit = tryWith(QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", query)){ qexec =>
        val results = qexec.execSelect()
        if (results.hasNext) {
          val soln = results.next
          val wikiList: util.ArrayList[String] = new util.ArrayList()
          wikiList.add(soln.getResource("s").toString)
          wikiList.add(soln.getLiteral("o").getLexicalForm)
          return wikiList
        }
      }
      return null
    }

    def getWikidataDescriptionQuery(trimmedLabel: String): String = {
      s"""PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |PREFIX schema: <http://schema.org/>
         |SELECT * WHERE {
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

