#!/bin/bash

 SUITE_TYPE=$1 
 BUILD_NUMBER=$2
 SUITE_NAME=$3
 MAJOR_VERSION=$4
 MINOR_VERSION=$5
 DEPLOY_ROOT_BIN_DIR=$6
 BUILD_LOG_URL=$7
 MAVEN_REPO_LOCAL=$8
 MAVEN_PROJECTS_VERSION_XAP=$9
 MAVEN_PROJECTS_VERSION_CLOUDIFY=${10}

pushd ../local-builds/${BUILD_NUMBER}/Cloudify-iTests

TYPE=cloudify

TEST_REPORT_CLASS=iTests.framework.testng.report.TestsReportMerger
WIKI_REPORT_CLASS=iTests.framework.testng.report.wiki.WikiReporter

if [ `echo ${MAJOR_VERSION} | awk '{split($0,a,"."); print a[1]}'` -lt 9 ]
then
    TEST_REPORT_CLASS=org.cloudifysource.quality.framework.testng.report.TestsReportMerger; export TEST_REPORT_CLASS;
    WIKI_REPORT_CLASS=org.cloudifysource.quality.framework.testng.report.wiki.WikiReporter; export WIKI_REPORT_CLASS;
elif [ `echo ${MAJOR_VERSION} | awk '{split($0,a,"."); print a[1]}'` -ge 10 ]
then
	export TEST_REPORT_CLASS;
	export WIKI_REPORT_CLASS;
else
     if [ `echo ${MAJOR_VERSION} | awk '{split($0,a,"."); print a[2]}'` -lt 6 ]
     then
        TEST_REPORT_CLASS=org.cloudifysource.quality.framework.testng.report.TestsReportMerger; export TEST_REPORT_CLASS;
        WIKI_REPORT_CLASS=org.cloudifysource.quality.framework.testng.report.wiki.WikiReporter; export WIKI_REPORT_CLASS;
     else
        export TEST_REPORT_CLASS;
        export WIKI_REPORT_CLASS;
     fi
fi

if [ ! -z `echo ${SUITE_NAME} | grep BigData` ] ; then TYPE=xap-premium; fi

CLOUDIFY_HOME=/export/tgrid/sgtest3.0-cloudify/deploy/local-builds/${BUILD_NUMBER}/gigaspaces-${TYPE}-${MAJOR_VERSION}-${MINOR_VERSION}

mvn exec:java -Dexec.mainClass="${TEST_REPORT_CLASS}" -Dexec.args="${SUITE_NAME} ../${SUITE_NAME} ../${SUITE_NAME}" -Dcloudify.home=${CLOUDIFY_HOME} -Dmaven.repo.local=${MAVEN_REPO_LOCAL} -DgsVersion=${MAVEN_PROJECTS_VERSION_XAP} -DcloudifyVersion=${MAVEN_PROJECTS_VERSION_CLOUDIFY}

mvn exec:java -Dexec.mainClass="${WIKI_REPORT_CLASS}" -Dexec.args="../${SUITE_NAME} ${SUITE_TYPE} ${BUILD_NUMBER} ${MAJOR_VERSION} ${MINOR_VERSION} ${BUILD_LOG_URL}" -Dcloudify.home=${CLOUDIFY_HOME} -Dmysql.host="192.168.9.44" -Dmysql.user="sa" -Dmysql.pass="" -Dmaven.repo.local=${MAVEN_REPO_LOCAL} -DgsVersion=${MAVEN_PROJECTS_VERSION_XAP} -DcloudifyVersion=${MAVEN_PROJECTS_VERSION_CLOUDIFY}

popd



#return java exit code. 
exit $?
