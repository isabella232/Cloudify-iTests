#!/bin/bash

set -e

umask 000

# user args
# --------------------------------------------------------------------------------------------------------------
# 1 arg build-argument i.e: build_1804-01, or no args to get full list of available builds on build-server
# 2 arg major version.
# 3 arg minor version.
# 4 build type (cloudify-premium, cloudify-free)
# 5 arg jdk version for Cloudify zip
# 6 arg jdk order version for config-java
# 7 arg client machine
# 8 arg gsa machines.
# 9 arg jvm properties
#
# @author Kobi
# @Date   7-April-2009
##

 BUILD_NUMBER=$1
 MAJOR_VERSION=$2
 MINOR_VERSION=$3
 PACKAGE_NAME=$4
 JAVA_JDK=$5
 CONFIG_JAVA_ORDER=$6; export CONFIG_JAVA_ORDER
 JVM_PROPERTIES=$7; export JVM_PROPERTIES
 SGTEST_CHECKOUT_FOLDER=$8; export SGTEST_CHECKOUT_FOLDER
 TARGET_GSA_WAN_MACHINES=$9
 # SGTEST_TYPE could be REGULAR, BACKWARDS
 SGTEST_TYPE=${10}; export SGTEST_TYPE
 # SGTEST_CLIENT_TYPE could be OLD_CLIENT or NEW_CLIENT
 SGTEST_CLIENT_TYPE=${11}; export SGTEST_CLIENT_TYPE
 BRANCH_NAME=${12}; export BRANCH_NAME
 INCLUDE=${13}; export INCLUDE
 EXCLUDE=${14}; export EXCLUDE
 SUITE_NAME=${15}; export SUITE_NAME
 SVN_BRANCH_DIRECTORY=${16}; export SVN_BRANCH_DIRECTORY
 SUITE_NUMBER=${17}
 ## total number of init parameters is 25
 
 declare -a target_client_machines=(${18} ${20} ${22} ${24});
 declare -a target_gsa_machines=(${19} ${21} ${23} ${25});
 BYON_MACHINES=${26}
 SUPPORTED_CLOUDS=${27}
 BUILD_LOG_URL=${28}; export BUILD_LOG_URL


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
 
/export/utils/ant/apache-ant-1.8.1/bin/ant -f ${DEPLOY_ROOT_BIN_DIR}/../../bin/pre-run.xml prepare

for ((id=0 ; id < ${SUITE_NUMBER} ; id++ )); do
 SUITE_ID=${id}
 LOOKUPGROUPS="${SG_LOOKUPGROUPS}"${SUITE_ID}; export LOOKUPGROUPS 

 SUITE_WORK_DIR=${BUILD_DIR}/${SUITE_NAME}${SUITE_ID}_work; export SUITE_WORK_DIR
 rm -rf ${SUITE_WORK_DIR}
 mkdir ${SUITE_WORK_DIR}	

 SUITE_DEPLOY_DIR=${BUILD_DIR}/${SUITE_NAME}${SUITE_ID}_deploy; export SUITE_DEPLOY_DIR
 rm -rf ${SUITE_DEPLOY_DIR}
 mkdir ${SUITE_DEPLOY_DIR}
 cp -R ${BUILD_DIR}/deploy/templates ${SUITE_DEPLOY_DIR}	

 SUITE_JVM_PROPERTIES="-Dcom.gs.work=${SUITE_WORK_DIR} -Dcom.gs.deploy=${SUITE_DEPLOY_DIR} ${JVM_PROPERTIES}"

 TARGET_CLIENT_MACHINE=${target_client_machines[$id]}
 TARGET_GSA_MACHINES=${target_gsa_machines[$id]}
 #participating machines list (sorted, not duplicated).
 TARGET_MACHINES_ARRAY=()
 #set array of target machines to TARGET_MACHINES_ARRAY.
 get_target_machines_array

 # copy cloudify premium license ro run cloudify xap suite
 if [ "${SUITE_NAME}" == "CLOUDIFY_XAP" ]
  then
       rm -rf ${BUILD_CACHE_DIR}/gigaspaces*.zip
       echo copy cloudify premium license ro run cloudify xap suite
 	cp ${BUILD_DIR}/gslicense.xml ${BUILD_DIR}/gslicense.xml.org
       cp ${DEPLOY_ROOT_BIN_DIR}/../../bin/gslicense.xml ${BUILD_DIR}      
 fi

 echo "participating machines are ${TARGET_MACHINES_ARRAY[@]}"

 echo "a. start a remote gs-agents execution on requested machine."
 #---------------------------------------------------------
 run_gsagents

 echo "b. start a remote sgtest execution on requested machine."
 #---------------------------------------------------------


 #delete result file
 CURRENT_RESULT_INDICATOR_FILE=${RESULT_INDICATOR_FILE}${SUITE_ID}
 if [ -f ${CURRENT_RESULT_INDICATOR_FILE} ];
 then
 	rm ${CURRENT_RESULT_INDICATOR_FILE}
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


 ${PDSH} -w ssh:pc-lab[${TARGET_CLIENT_MACHINE}] "${CLIENT_EXECUTOR_SCRIPT} ${DEPLOY_ROOT_BIN_DIR} ${SGTEST_CLIENT_BUILD_DIR} ${CONFIG_JAVA_ORDER} ${LOOKUPGROUPS} ${BUILD_NUMBER} ${INCLUDE} ${EXCLUDE} ${SUITE_NAME} ${MAJOR_VERSION} ${MINOR_VERSION} ${SUITE_ID} ${SUITE_NUMBER} ${BYON_MACHINES} ${SUPPORTED_CLOUDS} ${SUITE_WORK_DIR} ${SUITE_DEPLOY_DIR}" &

done
#end for

for ((s=0 ; s < ${SUITE_NUMBER} ; s++ )); do
 current_file=${RESULT_INDICATOR_FILE}${s}
 while [ ! -f ${current_file} ]
 do
 	#sleep 60 seconds and check again...
 	sleep 60
 done
done

 echo ---------------------------------------
 echo "    ### end sgtest ###   "
 echo " So Long, and Thanks for All the Fish..."
 echo ---------------------------------------

 EXIT_CODE=`grep "result" ${RESULT_INDICATOR_FILE}${s} | awk '{print $2}'`

 # change this to copy junit report!!!
 #copy updated excel file to output dir. add prefix of today's date to copied file name.
 SGTEST_DIR=${DEPLOY_ROOT_BIN_DIR}/../..
 if [ -f ${SGTEST_DIR}/${OUTPUTFILE_NAME} ];
 	then
 		cp ${SGTEST_DIR}/${OUTPUTFILE_NAME} ${SGTEST_DIR}/output/`date '+%d-%m-%y'`_${OUTPUTFILE_NAME}
 fi

 # f. kill all processes after sgtest returns.
 cd ${DEPLOY_ROOT_BIN_DIR}
for ((s=0 ; s < ${SUITE_NUMBER} ; s++ )); do
 TARGET_CLIENT_MACHINE=${target_client_machines[$s]}
 TARGET_GSA_MACHINES=${target_gsa_machines[$s]}
 clean_machines
done

${DEPLOY_ROOT_BIN_DIR}/../../bin/generate-report-cmd.sh Regression ${BUILD_NUMBER} ${SUITE_NAME} ${MAJOR_VERSION} ${MINOR_VERSION} ${DEPLOY_ROOT_BIN_DIR} ${BUILD_LOG_URL}

 exit ${EXIT_CODE}