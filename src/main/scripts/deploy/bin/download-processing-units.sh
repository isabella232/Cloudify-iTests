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

${Cloudify-iTests_HOME}=${BUILD_DIR}/../Cloudify-iTests

if [ -d "${Cloudify-iTests_HOME}" ]; then
    rm -rf ${Cloudify-iTests_HOME}
fi

mkdir ${BUILD_DIR}/../Cloudify-iTests
export GIT_SSL_NO_VERIFY=true
pushd ${BUILD_DIR}/../Cloudify-iTests
git clone --depth 1 https://github.com/CloudifySource/Cloudify-iTests.git
popd

mvn scm:export -DconnectionUrl=scm:svn:svn://svn-srv/SVN/cloudify/trunk/quality/frameworks/SGTest-credentials -DexportDirectory=${Cloudify-iTests_HOME}/src/main/resources/credentials

dos2unix ${Cloudify-iTests_HOME}/src/main/resources/apps/cloudify/cloud/**/*.sh

USM=${Cloudify-iTests_HOME}/src/main/resources/apps/USM
svn export ${SVN_STOCKDEMO_REPOSITORY} ${USM}/usm/applications/stockdemo --force

svn export ${SVN_STOCKDEMO_REPOSITORY}/stockAnalyticsMirror ${USM}/usm/stockAnalyticsMirror --force

cd $CURRENT_DIR

cp ${Cloudify-iTests_HOME}/src/main/config/hsqldb.xml ${BUILD_DIR}/config/gsa/

cp ${Cloudify-iTests_HOME}/src/main/config/apache.xml ${BUILD_DIR}/config/gsa/

cp ${Cloudify-iTests_HOME}/src/main/config/apache-lb.xml ${BUILD_DIR}/config/gsa/

cp ${Cloudify-iTests_HOME}/src/main/config/jboss.xml ${BUILD_DIR}/config/gsa/

cp ${Cloudify-iTests_HOME}/src/main/config/security/in-memory-security-config.xml ${BUILD_DIR}/config/security/

cp ${Cloudify-iTests_HOME}/src/main/config/security/custom-test-security.properties ${BUILD_DIR}/config/security/

cp ${Cloudify-iTests_HOME}/src/main/config/security/spring-test-security.properties ${BUILD_DIR}/config/security/

cp ${Cloudify-iTests_HOME}/src/main/config/security/sgtest_login.properties ${BUILD_DIR}/config/security/

cp ${Cloudify-iTests_HOME}/src/main/config/gs_logging.properties /export/tgrid/sgtest3.0-cloudify/config/
rm -f ${BUILD_DIR}/config/gs_logging.properties
cp ${Cloudify-iTests_HOME}/src/main/config/gs_logging.properties gs_logging.properties ${BUILD_DIR}/config/

rm -f ${BUILD_DIR}/config/sgtest_logging.properties
cp ${Cloudify-iTests_HOME}/src/main/config/sgtest_logging.properties /export/tgrid/sgtest3.0-cloudify/config/