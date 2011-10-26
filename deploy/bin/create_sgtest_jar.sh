#!/bin/bash

CURRENT_DIR=`pwd`

cd ${SGTEST_CHECKOUT_FOLDER}

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
