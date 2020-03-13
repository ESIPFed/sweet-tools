#!/usr/bin/env amm
//
// This script reads in *.ttl files from a given directory. For
// each owl:Class in each file, if the program encounters an rdfs:label, it 
// will execute the following query against wikidata
// 
// PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
// PREFIX schema: <http://schema.org/> 
// SELECT ?o WHERE { 
//   ?s rdfs:label "acceptable daily intake"@en . 
//   ?s schema:description ?o . 
//   FILTER(LANGMATCHES(LANG(?o), "en"))
//   FILTER(STRLEN(?o) > 15)
// }
//
// The above query selects all Resources which have an identical english
// rdfs:label and have an schema:description.
// 
// USAGE:
//       ./augment_wikidata_definitions.sc dirA
//

import $ivy.`org.slf4j:slf4j-nop:1.7.25`
import $ivy.`org.apache.jena:jena:3.14.0`
import $ivy.`org.apache.jena:jena-tdb:3.14.0`
import org.apache.jena.ontology.OntModel
import org.apache.jena.query.QueryExecution
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.ResultSet
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.impl.PropertyImpl
import org.apache.jena.riot.RDFDataMgr

import java.io.File

@main
def main(dir: String) {
  val dirFile = new File(dir)

  val files = dirFile
    .listFiles()
    .filter(_.getName.endsWith(".ttl"))
    .sortBy(_.getName)

  files foreach { file ⇒
    val model = loadModel(file)
    val owlClasses = model.listResourcesWithProperty(new PropertyImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), "http://www.w3.org/2002/07/owl#Class")
    while (owlClasses.hasNext) {
      val classResource = owlClasses.nextResource()
      if (classResource.hasProperty(new PropertyImpl("http://www.w3.org/2000/01/rdf-schema#label"))) {
        val label = classResource.getProperty(new PropertyImpl("http://www.w3.org/2000/01/rdf-schema#label", "en"))
        val wikidataDescription = executeWikidataDescriptionQuery(label.getLiteral().toString())
        //classResource.addLiteral()
      }
    }
  }

  def loadModel(file: File): OntModel = {
    try Some(RDFDataMgr.loadModel(file.getPath))
    catch {
      case e: Exception ⇒
        val parentName = file.getParentFile.getName
        val name = parentName + "/" + file.getName
        print(s"\n  ERROR: $name: ${e.getMessage}")
        None
    }
  }

  def executeWikidataDescriptionQuery(label: String): String = {
    val queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "PREFIX schema: <http://schema.org/> \n" +
                "SELECT ?o WHERE { \n" +
                "  ?s rdfs:label \"$label\"@en . \n" +
                "  ?s schema:description ?o . \n" +
                "  FILTER(LANGMATCHES(LANG(?o), \"en\"))\n" +
                "  FILTER(STRLEN(?o) > 15)\n" +
                "}";
    val query = QueryFactory.create(queryString) ;
    try {
      val qexec = QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", query)
      val results = qexec.execSelect();
      //for (result <- results.hasNext()) {
        //QuerySolution soln = results.nextSolution() ;
        //RDFNode x = soln.get("varName") ;       // Get a result variable by name.
        //Resource r = soln.getResource("VarR") ; // Get a result variable - must be a resource
        //Literal l = soln.getLiteral("VarL") ;   // Get a result variable - must be a literal
      //}
    }
  }
}