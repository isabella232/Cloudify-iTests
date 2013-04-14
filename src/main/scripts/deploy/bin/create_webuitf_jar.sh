#!/bin/bash

CURRENT_DIR=`pwd`
WEBUI_TMP_DIR=/tmp/webuitf
if [ -d "${WEBUI_TMP_DIR}" ]; then
   rm -rf ${WEBUI_TMP_DIR}
fi
mkdir ${WEBUI_TMP_DIR}
echo "cloning webuitf"

export GIT_SSL_NO_VERIFY=true
pushd ${WEBUI_TMP_DIR}
if [ ${BRANCH_NAME} != "trunk" ]; then
        git clone  -b ${BRANCH_NAME} --depth 1 https://github.com/CloudifySource/Cloudify-iTests-webuitf.git
else
        git clone --depth 1 https://github.com/CloudifySource/Cloudify-iTests-webuitf.git
fi

popd
export Cloudify_iTests_webuitf=${WEBUI_TMP_DIR}/Cloudify-iTests-webuitf

pushd ${Cloudify_iTests_webuitf}
mvn clean install s3client:deploy -U
rm -rf ${Cloudify_iTests_webuitf}
rm -rf ${WEBUI_TMP_DIR}
popd

cd ${CURRENT_DIR}