#!/bin/bash

 SUITE_TYPE=$1 
 BUILD_NUMBER=$2
 SUITE_NAME=$3
 MAJOR_VERSION=$4
 MINOR_VERSION=$5
 DEPLOY_ROOT_BIN_DIR=$6
 BUILD_LOG_URL=$7

pushd ../local-builds/${BUILD_NUMBER}/SGTest

TYPE=cloudify

if [ ! -z `echo ${SUITE_NAME} | grep BigData` ] ; then TYPE=xap-premium; fi

CLOUDIFY_HOME=/export/tgrid/sgtest3.0-cloudify/deploy/local-builds/${BUILD_NUMBER}/gigaspaces-${TYPE}-${MAJOR_VERSION}-${MINOR_VERSION}

mvn exec:java -Dexec.mainClass="framework.testng.report.TestsReportMerger" -Dexec.args="${SUITE_TYPE} ${BUILD_NUMBER} ${SUITE_NAME} ${MAJOR_VERSION} ${MINOR_VERSION}" -Dcloudify.home=${CLOUDIFY_HOME}

mvn exec:java -Dexec.mainClass="framework.testng.report.wiki.WikiReporter" -Dexec.args="${SUITE_TYPE} ${BUILD_NUMBER} ${SUITE_NAME} ${MAJOR_VERSION} ${MINOR_VERSION} ${BUILD_LOG_URL}" -Dcloudify.home=${CLOUDIFY_HOME}

popd



#return java exit code. 
exit $?