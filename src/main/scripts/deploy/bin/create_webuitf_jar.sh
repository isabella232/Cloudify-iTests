#!/bin/bash

CURRENT_DIR=`pwd`
WEBUI_TMP_DIR=${SGTEST_CHECKOUT_FOLDER}/apps/webuitf

echo "exporting webuitf"

if [ ${BRANCH_NAME} != "trunk" ]; then
	SVN_WEBUITF_REPOSITORY=svn://pc-lab14/SVN/xap/branches/${SVN_BRANCH_DIRECTORY}/${BRANCH_NAME}/quality/frameworks/webuitf	
else
	SVN_WEBUITF_REPOSITORY=svn://pc-lab14/SVN/xap/trunk/quality/frameworks/webuitf
fi

svn export ${SVN_WEBUITF_REPOSITORY} ${WEBUI_TMP_DIR} --force

pushd ${WEBUI_TMP_DIR}
mvn clean install s3client:deploy -U
rm -rf ${WEBUI_TMP_DIR}
popd

cd ${CURRENT_DIR}