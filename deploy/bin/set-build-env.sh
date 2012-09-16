#!/bin/bash

# - Set or override the JAVA_HOME variable
# - By default, the system value is used.
# JAVA_HOME="/utils/jdk1.5.0_01"
# - Reset JAVA_HOME unless JAVA_HOME is pre-defined.

if [ -z "${JAVA_HOME}" ]; then
  echo "The JAVA_HOME environment variable is not set. Using the java that is set in system path."
 JAVACMD=java
 else
  JAVACMD="${JAVA_HOME}/bin/java"
fi

# hostname
HOST=`hostname`; export HOST

#home directory of sgtest
#SGTEST_DEPLOY_DIR=`dirname $0`/..
SGTEST_DEPLOY_DIR=`pwd -P`/..



#according to this variables will be created a full path to the GS build
BUILD_VERSION=${MAJOR_VERSION}
BUILD_MILESTONE=${MINOR_VERSION}
BUILD_JDK_VER=${JAVA_JDK}
BUILD_TYPE=${PACKAGE_NAME}

# if not <null> set supplied jdk version otherwise use default from set-build-env.sh
if [ -z "${BUILD_JDK_VER}" ]
 then
   BUILD_JDK_VER=1.5
fi

BUILD_INSTALL_DIR=gigaspaces-${BUILD_TYPE}-${BUILD_VERSION}-${BUILD_MILESTONE}

# location to director with all regression builds
if [ ${BUILD_TYPE} == "cloudify" ]; then
	REGRESSION_BUILDS_PATH=/export/builds/cloudify/${BUILD_VERSION}
else
	REGRESSION_BUILDS_PATH=/export/builds/${BUILD_VERSION}
fi

#this directory contains all GS builds all ever ran by this user, every build downloaded to the specified directory and use locally!
BUILDS_CACHE_REPOSITORY=${SGTEST_DEPLOY_DIR}/local-builds

MAIL_ENABLED=false; export MAIL_ENABLED