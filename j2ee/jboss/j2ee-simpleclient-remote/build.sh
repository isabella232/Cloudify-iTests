#!/bin/bash

LOOKUPGROUPS=sgtest
.  ${BUILD_DIR}/bin/setenv.sh

echo "${ANT_JARS}":"${JAVA_HOME}"/lib/tools.jar

java "${LOOKUP_GROUPS_PROP}" -classpath "${ANT_JARS}":"${JAVA_HOME}"/lib/tools.jar org.apache.tools.ant.Main $1
