#!/bin/bash

# This scripts provide steps to start/kill GS-AGENTS on desired machines.
# Available operations:
# 1. Start process with desired JDK vendor.
# 3. Kill process.
#
# @author Kobi
# @date	  7-April-2008

 # setup env variables and common functions
 . set-deploy-env.sh

 OPERATION=$1
 LOC_REMOTE_MACHINES=$2
 GSA_TYPE=$3


 if [ "${SGTEST_TYPE}" == "${BACKWARDS_SGTEST_TYPE}" ] 
  then if [ "${SGTEST_CLIENT_TYPE}" == "${SGTEST_OLD_CLIENT_TYPE}" ] 
        then
	  # IF OLD_CLIENt, so the server must be new, and vice versa
          LOC_BUILD_DIR=${BUILD_DIR}
        else
          LOC_BUILD_DIR=${OLD_BUILD_DIR}
        fi
  else
   	LOC_BUILD_DIR=${BUILD_DIR}
 fi

 LOC_CONFIG_JAVA_ORDER=${CONFIG_JAVA_ORDER}
 LOC_JVM_PROPERTIES="${SUITE_JVM_PROPERTIES}"

 # 1 OPERATION
 # 2 CONFIG_JAVA_ORDER
 # 3 DEPLOY_ROOT_BIN_DIR
 # 4 BUILD_DIR
 # 5 GSA_TYPE
 # 6 LOOKUPGROUPS
 # 7 JVM_PROPERTIES


 # execute supplied remote commands on desired lab machines
if [ "${OPERATION}" == "1" ] 
then
  ${PDSH} -w ssh:pc-lab[${LOC_REMOTE_MACHINES}] "${REMOTE_EXECUTOR_SCRIPT} ${OPERATION} ${LOC_CONFIG_JAVA_ORDER} ${DEPLOY_ROOT_BIN_DIR} ${LOC_BUILD_DIR} ${GSA_TYPE} ${LOOKUPGROUPS} ${LOC_JVM_PROPERTIES}"	
else
 ${PDSH} -u 10 -w ssh:pc-lab[${LOC_REMOTE_MACHINES}] "${REMOTE_EXECUTOR_SCRIPT} ${OPERATION} ${LOC_CONFIG_JAVA_ORDER} ${DEPLOY_ROOT_BIN_DIR} ${LOC_BUILD_DIR} ${GSA_TYPE} ${LOOKUPGROUPS} ${LOC_JVM_PROPERTIES}"

fi