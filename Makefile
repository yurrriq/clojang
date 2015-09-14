jinterface:
	mvn install:install-file \
	-Durl=file:repo \
	-DgroupId=com.ericsson.otp.erlang \
	-DartifactId=otperlang \
	-Dversion=$(JINTERFACE_VER) -Dpackaging=jar \
	-Dfile=$(ERL_LIBS)/lib/jinterface-$(JINTERFACE_VER)/priv/OtpErlang.jar
