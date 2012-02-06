#!/bin/bash

umask 000

# user args
# --------------------------------------------------------------------------------------------------------------
# 1 arg build-argument i.e: build_1804-01, or no args to get full list of available builds on build-server
# 2 arg major version.
# 3 arg minor version.
# 4 build type (xap-premium, xap-standard, xap-community)
# 5 arg jdk version for XAP zip
# 6 arg jdk order version for config-java
# 7 arg client machine
# 8 arg gsa machines.
# 9 arg jvm properties
#
# @author Kobi
# @Date   7-April-2009
##

 BUILD_NUMBER=$1
 MAJOR_VERSION=$2; export MAJOR_VERSION
 MINOR_VERSION=$3; export MINOR_VERSION
 PACKAGE_NAME=$4
 JAVA_JDK=$5
 CONFIG_JAVA_ORDER=$6; export CONFIG_JAVA_ORDER
 TARGET_CLIENT_MACHINE=$7
 TARGET_GSA_MACHINES=${8}
 JVM_PROPERTIES=$9; export JVM_PROPERTIES
 SGTEST_CHECKOUT_FOLDER=${10}; export SGTEST_CHECKOUT_FOLDER
 TARGET_GSA_WAN_MACHINES=${11}
 # SGTEST_TYPE could be REGULAR, BACKWARDS
 SGTEST_TYPE=${12}; export SGTEST_TYPE
 # SGTEST_CLIENT_TYPE could be OLD_CLIENT or NEW_CLIENT
 SGTEST_CLIENT_TYPE=${13}; export SGTEST_CLIENT_TYPE
 BRANCH_NAME=${14}; export BRANCH_NAME
 INCLUDE=${15}; export INCLUDE
 EXCLUDE=${16}; export EXCLUDE
 SUITE_NAME=${17}; export SUITE_NAME
 
 . set-deploy-env.sh

 #setup lookup group
 if [ "$SG_LOOKUPGROUPS" == "" ]
  then if [ "${SGTEST_TYPE}" == "${BACKWARDS_SGTEST_TYPE}" ] 
	then
	  SG_LOOKUPGROUPS=backwards-sgtest-cloudify
	else
	  SG_LOOKUPGROUPS=sgtest-cloudify
	fi
 fi
 LOOKUPGROUPS="${SG_LOOKUPGROUPS}"; export LOOKUPGROUPS

 #setup functions
 . functions.sh $*

 echo "> Setup build"
 . setup-build.sh
 
 BUILD_DIR=${BUILD_CACHE_DIR}/${BUILD_INSTALL_DIR}; export BUILD_DIR
 OLD_BUILD_DIR=/export/tgrid/sgtest/deploy/local-builds/build_5000/gigaspaces-xap-premium-8.0.0-ga; export OLD_BUILD_DIR
 
 if [ -d ${SGTEST_CHECKOUT_FOLDER} ];
 then
 	. create_sgtest_jar.sh
 fi 

 echo "> Downloading jars/wars"
 . download-processing-units.sh
 
 #participating machines list (sorted, not duplicated).
 TARGET_MACHINES_ARRAY=()
 #set array of target machines to TARGET_MACHINES_ARRAY.
 get_target_machines_array

 echo "participating machines are ${TARGET_MACHINES_ARRAY[@]}"

 echo "a. start a remote gs-agents execution on requested machine."
 #---------------------------------------------------------
 run_gsagents

 echo "b. start a remote sgtest execution on requested machine."
 #---------------------------------------------------------


 #delete result file
 if [ -f ${RESULT_INDICATOR_FILE} ];
 then
 	rm ${RESULT_INDICATOR_FILE}
 fi

 echo ---------------------------------------
 echo "   ### start sgtest ###  "
 echo ---------------------------------------

CLIENT_EXECUTOR_SCRIPT="${DEPLOY_ROOT_BIN_DIR}/client-sgtest-executor.sh"

  if [ "${SGTEST_TYPE}" == "${BACKWARDS_SGTEST_TYPE}" ]
  then if [ "${SGTEST_CLIENT_TYPE}" == "${SGTEST_OLD_CLIENT_TYPE}" ]
        then
          SGTEST_CLIENT_BUILD_DIR=${OLD_BUILD_DIR}
        else
          SGTEST_CLIENT_BUILD_DIR=${BUILD_DIR}
        fi
  else
	SGTEST_CLIENT_BUILD_DIR=${BUILD_DIR}
 fi

if [ "${SUITE_NAME}" == "Cloudify_XAP" ]
 then
 	sleep 1
fi

 ${PDSH} -w ssh:pc-lab[${TARGET_CLIENT_MACHINE}] "${CLIENT_EXECUTOR_SCRIPT} ${DEPLOY_ROOT_BIN_DIR} ${SGTEST_CLIENT_BUILD_DIR} ${CONFIG_JAVA_ORDER} ${LOOKUPGROUPS} ${BUILD_NUMBER} ${INCLUDE} ${EXCLUDE} ${SUITE_NAME} ${MAJOR_VERSION} ${MINOR_VERSION} &"

 while [ ! -f ${RESULT_INDICATOR_FILE} ]
 do
 	#sleep 60 seconds and check again...
 	sleep 60
 done

 echo ---------------------------------------
 echo "    ### end sgtest ###   "
 echo " So Long, and Thanks for All the Fish..."
 echo ---------------------------------------

 EXIT_CODE=`grep "result" ${RESULT_INDICATOR_FILE} | awk '{print $2}'`

 # change this to copy junit report!!!
 #copy updated excel file to output dir. add prefix of today's date to copied file name.
 SGTEST_DIR=${DEPLOY_ROOT_BIN_DIR}/../..
 if [ -f ${SGTEST_DIR}/${OUTPUTFILE_NAME} ];
 	then
 		cp ${SGTEST_DIR}/${OUTPUTFILE_NAME} ${SGTEST_DIR}/output/`date '+%d-%m-%y'`_${OUTPUTFILE_NAME}
 fi

 # f. kill all processes after sgtest returns.
 cd ${DEPLOY_ROOT_BIN_DIR}
 clean_machines

 exit ${EXIT_CODE}
