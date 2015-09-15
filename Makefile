PROJ := $(strip $(word 2, $(shell grep defproject project.clj )))
ERL_LIBS := $(shell erl -eval "io:format(code:root_dir()),halt()" -noshell)
JINTERFACE := $(shell ls -1 $(ERL_LIBS)/lib/|grep jinterface)
JINTERFACE_VER := $(word 2, $(subst -, , $(JINTERFACE)))
VERSION := $(strip $(subst \", , $(word 3, $(shell grep defproject project.clj ))))
JAR := $(PROJ)-$(VERSION).jar
UBERJAR := $(PROJ)-$(VERSION)-standalone.jar
LOCAL_MAVEN := ~/.m2/repository

debug:
	@echo $(PROJ)
	@echo $(ERL_LIBS)
	@echo $(JINTERFACE)
	@echo $(JINTERFACE_VER)
	@echo $(VERSION)
	@echo $(JAR)

jinterface:
	mvn deploy:deploy-file \
	-Durl=file:repo \
	-DgroupId=org.erlang.otp \
	-DartifactId=jinterface \
	-Dversion=$(JINTERFACE_VER) \
	-Dpackaging=jar \
	-Dfile=$(ERL_LIBS)/lib/jinterface-$(JINTERFACE_VER)/priv/OtpErlang.jar

local:
	lein jar
	mvn deploy:deploy-file \
	-Durl=file:repo \
	-Dfile=target/$(JAR) \
	-DgroupId=self \
	-DartifactId=$(PROJ) \
	-Dversion=$(VERSION) \
	-Dpackaging=jar \
	-DgeneratePom=true \
	-DcreateChecksum=true \
	-DlocalRepositoryPath=$(LOCAL_MAVEN)

local-standalone:
	lein uberjar
	mvn deploy:deploy-file \
	-Durl=file:repo \
	-Dfile=target/$(UBERJAR) \
	-DgroupId=self \
	-DartifactId=$(PROJ) \
	-Dversion=$(VERSION) \
	-Dpackaging=jar \
	-DgeneratePom=true \
	-DcreateChecksum=true \
	-DlocalRepositoryPath=$(LOCAL_MAVEN)
