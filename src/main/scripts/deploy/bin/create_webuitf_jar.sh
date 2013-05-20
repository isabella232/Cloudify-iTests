#!/bin/bash

WEBUI_TMP_DIR=${BUILD_DIR}/..
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

Cloudify_iTests_webuitf=${WEBUI_TMP_DIR}/Cloudify-iTests-webuitf

pushd ${Cloudify_iTests_webuitf}
mvn clean install s3client:deploy -U
for ((id=0 ; id < ${SUITE_NUMBER} ; id++ )); do
 cp -R target target${SUITE_NAME}${id}
done
popd
