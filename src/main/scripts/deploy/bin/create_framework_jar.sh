#!/bin/bash

FRAMEWORK_TMP_DIR=${BUILD_DIR}/../framework
if [ -d "${FRAMEWORK_TMP_DIR}" ]; then
   rm -rf ${FRAMEWORK_TMP_DIR}
fi
mkdir ${FRAMEWORK_TMP_DIR}
echo "cloning framework"

export GIT_SSL_NO_VERIFY=true
pushd ${FRAMEWORK_TMP_DIR}
if [ ${BRANCH_NAME} != "trunk" ]; then
        git clone  -b ${BRANCH_NAME} --depth 1 https://github.com/CloudifySource/iTests-Framework.git
else
        git clone --depth 1 https://github.com/CloudifySource/iTests-Framework.git
fi

popd
iTests_Framework=${FRAMEWORK_TMP_DIR}/iTests-Framework

echo "deploying framework"
pushd ${iTests_Framework}
mvn clean install s3client:deploy -U -Dbuild.home=${BUILD_DIR} -Dmaven.repo.local=${MAVEN_REPO_LOCAL}

rm -rf ${FRAMEWORK_TMP_DIR}
popd