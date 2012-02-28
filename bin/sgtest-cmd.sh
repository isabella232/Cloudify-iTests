#!/bin/bash

 BUILD_NUMBER=$1 
 INCLUDE=$2
 EXCLUDE=$3
 SUITE_NAME=$4
 MAJOR_VERSION=$5
 MINOR_VERSION=$6
 SUITE_ID=$7
 SUITE_NUMBER=$8

/export/utils/ant/apache-ant-1.8.1/bin/ant -d -DBUILD_DIR=${BUILD_DIR} -DBUILD_NUMBER=${BUILD_NUMBER} -DINCLUDE=${INCLUDE} -DEXCLUDE=${EXCLUDE} -DSUITE_NAME=${SUITE_NAME} -DMAJOR_VERSION=${MAJOR_VERSION} -DMINOR_VERSION=${MINOR_VERSION} -DSUITE_ID=${SUITE_ID} -DSUITE_NUMBER=${SUITE_NUMBER} -f run.xml testsummary

#return java exit code. 
exit $?
