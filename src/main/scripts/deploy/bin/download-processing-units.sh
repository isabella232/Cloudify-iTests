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

 ARCHIVES=${DEPLOY_ROOT_BIN_DIR}/../../apps/archives
 CURRENT_DIR=`pwd`
 cd $ARCHIVES
 rm -rf *

CLOUDIFY=${DEPLOY_ROOT_BIN_DIR}/../../apps/cloudify
cd $CLOUDIFY
rm -rf *

USM=${DEPLOY_ROOT_BIN_DIR}/../../apps/USM
cd $USM
rm -rf *


svn export ${SVN_SGTEST_REPOSITORY}/apps/archives ${ARCHIVES} --force

svn export ${SVN_SGTEST_REPOSITORY}/apps/cloudify ${CLOUDIFY} --force
dos2unix ${DEPLOY_ROOT_BIN_DIR}/../../apps/cloudify/cloud/**/*.sh

svn export ${SVN_SGTEST_REPOSITORY}/apps/USM ${USM} --force

svn export ${SVN_STOCKDEMO_REPOSITORY} ${USM}/usm/applications/stockdemo --force

svn export ${SVN_STOCKDEMO_REPOSITORY}/stockAnalyticsMirror ${USM}/usm/stockAnalyticsMirror --force

cd $CURRENT_DIR

#kobi
svn export ${SVN_SGTEST_REPOSITORY}/config/hsqldb.xml
mv hsqldb.xml ${BUILD_DIR}/config/gsa

svn export ${SVN_SGTEST_REPOSITORY}/config/apache.xml
mv apache.xml ${BUILD_DIR}/config/gsa

svn export ${SVN_SGTEST_REPOSITORY}/config/apache-lb.xml
mv apache-lb.xml ${BUILD_DIR}/config/gsa

svn export ${SVN_SGTEST_REPOSITORY}/config/jboss.xml
mv jboss.xml ${BUILD_DIR}/config/gsa

svn export ${SVN_SGTEST_REPOSITORY}/config/security/in-memory-security-config.xml
mv in-memory-security-config.xml ${BUILD_DIR}/config/security

svn export ${SVN_SGTEST_REPOSITORY}/config/security/custom-test-security.properties
mv custom-test-security.properties ${BUILD_DIR}/config/security

svn export ${SVN_SGTEST_REPOSITORY}/config/security/spring-test-security.properties
mv spring-test-security.properties ${BUILD_DIR}/config/security

svn export ${SVN_SGTEST_REPOSITORY}/config/security/sgtest_login.properties
mv sgtest_login.properties ${BUILD_DIR}/config/security

svn export ${SVN_SGTEST_REPOSITORY}/config/gs_logging.properties
rm -f ${BUILD_DIR}/config/gs_logging.properties
cp gs_logging.properties /export/tgrid/sgtest2.0-cloudify/config
mv gs_logging.properties ${BUILD_DIR}/config

svn export ${SVN_SGTEST_REPOSITORY}/config/sgtest_logging.properties
rm -f ${BUILD_DIR}/config/sgtest_logging.properties
cp sgtest_logging.properties /export/tgrid/sgtest2.0-cloudify/config


rm -rf /export/tgrid/sgtest2.0-cloudify/lib/xenserver/
svn --force export ${SVN_SGTEST_REPOSITORY}/lib/xenserver
mv xenserver /export/tgrid/sgtest2.0-cloudify/lib/

rm -rf /export/tgrid/sgtest2.0-cloudify/lib/selenium/
svn --force export ${SVN_SGTEST_REPOSITORY}/lib/selenium
mv selenium /export/tgrid/sgtest2.0-cloudify/lib/