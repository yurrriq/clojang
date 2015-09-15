PROJ := $(strip $(word 2, $(shell grep defproject project.clj )))
ERL_LIBS := $(shell erl -eval "io:format(code:root_dir()),halt()" -noshell)
JINTERFACE := $(shell ls -1 $(ERL_LIBS)/lib/|grep jinterface)
JINTERFACE_VER := $(strip $(subst \", , $(word 2, $(subst -, , $(JINTERFACE)))))
JINTERFACE_JAR := jinterface-$(JINTERFACE_VER).jar
VERSION := $(strip $(subst \", , $(word 3, $(shell grep defproject project.clj ))))
JAR := $(PROJ)-$(VERSION).jar
UBERJAR := $(PROJ)-$(VERSION)-standalone.jar
LOCAL_MAVEN := ~/.m2/repository
JINTERFACE_BUILD := /tmp/jinterface/$(JINTERFACE_VER)

debug:
	@echo $(PROJ)
	@echo $(ERL_LIBS)
	@echo $(JINTERFACE)
	@echo $(JINTERFACE_VER)
	@echo $(VERSION)
	@echo $(JAR)

clean-jinterface-build:
ifeq ($(strip $(JINTERFACE_BUILD)),)
	echo
else
	rm -rf $(JINTERFACE_BUILD)
endif

build-jinterface: clean-jinterface-build
	mkdir -p $(JINTERFACE_BUILD)/src
	cp -r $(ERL_LIBS)/lib/jinterface-$(JINTERFACE_VER)/java_src \
	 $(JINTERFACE_BUILD)/src/java
	cat ./resources/project.tmpl | \
	sed 's/{{VERSION}}/$(JINTERFACE_VER)/g' > \
	$(JINTERFACE_BUILD)/project.clj
	cd $(JINTERFACE_BUILD) && lein jar

jinterface: build-jinterface
	mvn deploy:deploy-file \
	-Durl=file:repo \
	-DgroupId=org.erlang.otp \
	-DartifactId=jinterface \
	-Dversion=$(JINTERFACE_VER) \
	-Dpackaging=jar \
	-Dfile=$(JINTERFACE_BUILD)/target/$(JINTERFACE_JAR)

jinterface-local-old: build-jinterface
	mvn deploy:deploy-file \
	-Durl=file:$(LOCAL_MAVEN) \
	-Dfile=$(JINTERFACE_BUILD)/target/$(JINTERFACE_JAR) \
	-DgroupId=org.erlang.otp \
	-DartifactId=jinterface \
	-Dversion=$(JINTERFACE_VER) \
	-Dpackaging=jar \
	-DgeneratePom=true \
	-DcreateChecksum=true \
	-DlocalRepositoryPath=$(LOCAL_MAVEN)
	#cp $(JINTERFACE_BUILD)/target/$(JINTERFACE_JAR) \
	#$(LOCAL_MAVEN)/org/erlang/otp/jinterface/$(JINTERFACE_VER)/

jinterface-local: build-jinterface
	cd $(JINTERFACE_BUILD) && lein install

local:
	lein jar && lein install

local-standalone:
	lein uberjar && install
