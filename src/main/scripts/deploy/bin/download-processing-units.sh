#!/bin/bash

 . set-deploy-env.sh

if [ ${BRANCH_NAME} != "trunk" ]; then
	SVN_SGTEST_REPOSITORY=svn://pc-lab14/SVN/cloudify/branches/${SVN_BRANCH_DIRECTORY}/${BRANCH_NAME}/quality/frameworks/SGTest	
else
	SVN_SGTEST_REPOSITORY=svn://pc-lab14/SVN/cloudify/trunk/quality/frameworks/SGTest
fi

SVN_STOCKDEMO_REPOSITORY=svn://pc-lab14/SVN/cloudify/trunk/cloudify/recipes/src/main/resources/recipes/apps/stockdemo

# set local build directory with entered build number
BUILD_CACHE_DIR=${BUILDS_CACHE_REPOSITORY}/${BUILD_NUMBER}

if [ -d "${BUILD_DIR}/../SGTest" ]; then
    rm -rf ${BUILD_DIR}/../SGTest
fi

svn export ${SVN_SGTEST_REPOSITORY} ${BUILD_DIR}/../SGTest

#dos2unix ${BUILD_DIR}/../SGTest/src/main/resources/apps/cloudify/cloud/**/*.sh

#svn export ${SVN_STOCKDEMO_REPOSITORY} ${USM}/usm/applications/stockdemo --force

#svn export ${SVN_STOCKDEMO_REPOSITORY}/stockAnalyticsMirror ${USM}/usm/stockAnalyticsMirror --force

cd $CURRENT_DIR

#kobi
svn export ${SVN_SGTEST_REPOSITORY}/src/main/config/hsqldb.xml
mv hsqldb.xml ${BUILD_DIR}/config/gsa

svn export ${SVN_SGTEST_REPOSITORY}/src/main/config/apache.xml
mv apache.xml ${BUILD_DIR}/config/gsa

svn export ${SVN_SGTEST_REPOSITORY}/src/main/config/apache-lb.xml
mv apache-lb.xml ${BUILD_DIR}/config/gsa

svn export ${SVN_SGTEST_REPOSITORY}/src/main/config/jboss.xml
mv jboss.xml ${BUILD_DIR}/config/gsa

svn export ${SVN_SGTEST_REPOSITORY}/src/main/config/security/in-memory-security-config.xml
mv in-memory-security-config.xml ${BUILD_DIR}/config/security

svn export ${SVN_SGTEST_REPOSITORY}/src/main/config/security/custom-test-security.properties
mv custom-test-security.properties ${BUILD_DIR}/config/security

svn export ${SVN_SGTEST_REPOSITORY}/src/main/config/security/spring-test-security.properties
mv spring-test-security.properties ${BUILD_DIR}/config/security

svn export ${SVN_SGTEST_REPOSITORY}/src/main/config/security/sgtest_login.properties
mv sgtest_login.properties ${BUILD_DIR}/config/security

svn export ${SVN_SGTEST_REPOSITORY}/src/main/config/gs_logging.properties
rm -f ${BUILD_DIR}/config/gs_logging.properties
cp gs_logging.properties /export/tgrid/sgtest2.0-cloudify/config
mv gs_logging.properties ${BUILD_DIR}/config

svn export ${SVN_SGTEST_REPOSITORY}/src/main/config/sgtest_logging.properties
rm -f ${BUILD_DIR}/config/sgtest_logging.properties
cp sgtest_logging.properties /export/tgrid/sgtest2.0-cloudify/config