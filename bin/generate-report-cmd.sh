#!/bin/bash

 SUITE_TYPE=$1 
 BUILD_NUMBER=$2
 SUITE_NAME=$3
 MAJOR_VERSION=$4
 MINOR_VERSION=$5
 DEPLOY_ROOT_BIN_DIR=$6
 BUILD_LOG_URL=$7

/export/utils/ant/apache-ant-1.8.1/bin/ant -d -DSUITE_TYPE=${SUITE_TYPE} -DBUILD_NUMBER=${BUILD_NUMBER} -DSUITE_NAME=${SUITE_NAME} -DMAJOR_VERSION=${MAJOR_VERSION} -DMINOR_VERSION=${MINOR_VERSION} -DBUILD_LOG_URL=${BUILD_LOG_URL} -f ${DEPLOY_ROOT_BIN_DIR}/../../bin/post-run.xml report-merger

/export/utils/ant/apache-ant-1.8.1/bin/ant -d -DSUITE_TYPE=${SUITE_TYPE} -DBUILD_NUMBER=${BUILD_NUMBER} -DSUITE_NAME=${SUITE_NAME} -DMAJOR_VERSION=${MAJOR_VERSION} -DMINOR_VERSION=${MINOR_VERSION} -f ${DEPLOY_ROOT_BIN_DIR}/../../bin/post-run.xml wiki-reporter


#return java exit code. 
exit $?
