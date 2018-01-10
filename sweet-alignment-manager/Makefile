## Makefile for building alignment rdf graphs
## Please consult README.md for more information.
## The only configuration should be the 'ONTS' constant.

# ----------------------------------------
# CONSTANTS
# ----------------------------------------
SWEET_REPO = sweet
CAT = $(SWEET_REPO)/src/catalog-v001.xml
SRC = $(SWEET_REPO)/src/sweetAll.ttl
AML_JAR = AML-Jar
TARGET = target
ONTS = datasetontology prov-o sosa ssn

VERBOSE = -v

# ----------------------------------------
# TOP LEVEL TARGET
# ----------------------------------------
all: align_all
setup: sweet aml
clean:
	rm alignments/*

# clone sweet into this repo.
# you can place this anywhere and symlink if you prefer
sweet:
	git clone https://github.com/ESIPFed/sweet.git

# clone AgreementMakerLight/AML-Jar into this repo
aml:
	git clone https://github.com/AgreementMakerLight/AML-Jar.git 

# ----------------------------------------
# Alignment
# ----------------------------------------

align_all: $(patsubst %,%,$(ONTS))

%:
	java -jar ${AML_JAR}/AgreementMakerLight.jar -s ${SRC} -t ${TARGET}/$@.ttl -o alignments/align-sweet-$@.rdf -a
