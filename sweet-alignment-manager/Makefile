## Makefile for building alignment rdf graphs
## Please consult README.md for more information.
## The only configuration should be the 'ONTS' constant.

# ----------------------------------------
# CONSTANTS
# ----------------------------------------
SWEET_REPO = sweet
CAT = $(SWEET_REPO)/src/catalog-v001.xml
SRC = $(SWEET_REPO)/src/sweetAll.ttl
AML_JAR = AML_v3.1
TARGET = target
ONTS = acInst acManufInst acModelInst airlineInst airportInst ARTCCLocationInst ASPMinst ATM ATMinst atmonto atmontoCore atmontoPlus data dataInst dayInst equipment equipmentInst fixInst flightInst general METARinst NAS NASinst routeInst SectorLocationInst sidStarInst TAFinst TMIinst TRACONinst

VERBOSE = -v

# ----------------------------------------
# TOP LEVEL TARGET
# ----------------------------------------
all: align_all
setup: sweet aml align_all
clean:
	rm alignments/*

# clone sweet into this repo.
# you can place this anywhere and symlink if you prefer
sweet:
	git clone https://github.com/ESIPFed/sweet.git

# clone AgreementMakerLight/AML-Jar into this repo
aml:
	curl -LO https://github.com/AgreementMakerLight/AML-Project/releases/download/v3.1/AML_v3.1.zip && unzip AML_v3.1.zip

# ----------------------------------------
# Alignment
# ----------------------------------------

align_all: $(patsubst %,%,$(ONTS))

%:
	java -jar ${AML_JAR}/AgreementMakerLight.jar -s ${SRC} -t ${TARGET}/$@.ttl -o alignments/align-sweet-$@.rdf -a
