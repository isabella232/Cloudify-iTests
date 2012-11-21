#!/bin/bash

CURRENT_DIR=`pwd`
WEBUI_TMP_DIR=${SGTEST_CHECKOUT_FOLDER}/apps/webuitf

cd ${SGTEST_CHECKOUT_FOLDER}
echo "exporting webuitf"

if [ ${BRANCH_NAME} != "trunk" ]; then
	SVN_WEBUITF_REPOSITORY=svn://pc-lab14/SVN/xap/branches/${SVN_BRANCH_DIRECTORY}/${BRANCH_NAME}/quality/frameworks/webuitf	
else
	SVN_WEBUITF_REPOSITORY=svn://pc-lab14/SVN/xap/trunk/quality/frameworks/webuitf
fi

mkdir ${WEBUI_TMP_DIR}
svn export ${SVN_WEBUITF_REPOSITORY} ${WEBUI_TMP_DIR} --force

echo "Compiling SGTest"
ant -d -f ${SGTEST_CHECKOUT_FOLDER}/build.xml -DgsHome.dir=${BUILD_DIR} jar1_6

echo "Copying ${SGTEST_CHECKOUT_FOLDER}/output/gs-sgtest.jar to ${SGTEST_ROOT_DIR}/lib"
if [ -f ${SGTEST_ROOT_DIR}/lib/gs-sgtest.jar ]
then 
    rm -f ${SGTEST_ROOT_DIR}/lib/gs-sgtest.jar
fi
cp ${SGTEST_CHECKOUT_FOLDER}/output/gs-sgtest.jar ${SGTEST_ROOT_DIR}/lib
chmod 755 ${SGTEST_ROOT_DIR}/lib/gs-sgtest.jar

cd ${CURRENT_DIR}