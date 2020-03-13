#!/usr/bin/env amm
//
// This script reads in *.ttl files from a given directory. For
// each owl:Class in each file, if the program encounters an rdfs:label, it 
// will execute the following query against wikidata
//
// PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
// PREFIX schema: <http://schema.org/>
// SELECT ?o WHERE {
//  ?s rdfs:label "behavior"@en .
//  ?s schema:description ?o .
//  FILTER(LANGMATCHES(LANG(?o), "en")) 
//
// The above query selects all Resources which have an identical english
// rdfs:label and have an schema:description.
// 
// USAGE:
//       ./augment_wikidata_definitions.sc dirA
//
@main
def main(dir: String) {
  val dirFile = new File(dir)

  val files = dirFile
    .listFiles()
    .filter(_.getName.endsWith(".ttl"))
    .sortBy(_.getName)

  files foreach { file ⇒
    val modelOpt = loadModel(file)
    val owlClasses = modelOpt.listResourcesWithProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://www.w3.org/2002/07/owl#Class")
    while (owlClasses.hasNext) {
      val classResource = owlClasses.nextResource()
      if classResource.hasProperty("http://www.w3.org/2000/01/rdf-schema#label")
        val label = classResource.getProperty("http://www.w3.org/2000/01/rdf-schema#label", "en")
        val wikidataDescription = executeWikidataDescriptionQuery(label)
        classResource.addLiteral()
    }
  }

  def loadModel(file: File): Option[Model] = {
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
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
    PREFIX schema: <http://schema.org/>
    SELECT ?o WHERE {
      ?s rdfs:label "behavior"@en .
      ?s schema:description ?o .
      FILTER(LANGMATCHES(LANG(?o), "en"))
    }
  }



