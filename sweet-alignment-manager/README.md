# SWEET Alignment Manager (SAM)

This repo is an attempt to create a **generic alignment manager** e.g. which can be used for the task of generic ontology alignments, providing a pipeline and results for aligning SWEET with various ontologies.

This is semi-automated but the goal is to learn from a seed of initial curated equivalence axioms using the [Agreement Maker Light](https://github.com/AgreementMakerLight/AML-Jar) (AML). AML is a lightweight ontology matching system specialized on the biomedical domain but applicable to any ontologies. It can be used to generate alignments automatically, as a platform for reviewing alignments, or as an alignment repair system (both automatically and interactively). Bu default, AML is configured within the SAM to generate alignments automatically. More information can be found below. 

## Prerequisites and Configuration

In order to use the SAM you must 1) obtain target mapping ontology(ies) and 2) include them in the Makefile

### Obtain target mapping ontology(ies)

Simply download the target ontology(ies) to the **target** directory... thats it, then

### Configure the Makefile

You must now edit ***Makefile*** and list the files which now reside in **target**. This can be done by assigning them (minus the file suffix if one exists) to the **ONTS** constant e.g. if **target** contains the files ***sosa.ttl*** and ***ssn.ttl***, the **ONTS** constant value should be as follows
```
ONTS = sosa ssn
```
Once that is done, you can continue to running the pipeline

## Running the pipeline

```
make setup
make
```

On subsequent runs, you do not need `make setup`. This clones [SWEET](https://github.com/esipfed/sweet) and [Agreement Maker Light](https://github.com/AgreementMakerLight/AML-Jar) into this repo so is only needed once.

## Results

By default, the pipeline WILL fail in its entirety, this is OK. 
If you have configured the pipeline correctly, the preliminary alignment outputs will be available in the **alignments** directory. They are written as and RDF graph.

## Acknowledgements

SAM uses The AgreementMakerLight ontology matching system, for more information see

- D. Faria, C. Pesquita, E. Santos, M. Palmonari, I. Cruz, and F. Couto, The AgreementMakerLight ontology matching system, ODBASE 2013. 

## License

SWEET Alignment Manager is licensed permissively under the [Apache License v2.0](https://www.apache.org/licenses/LICENSE-2.0)
a copy of which ships with this source code.

