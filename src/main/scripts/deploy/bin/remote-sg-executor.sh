#!/bin/sh -x

##
# This script provides remote command execution of gigaspacs services (gs-agent).
#
# @author Kobi
# @date	  7-April-2009
##

OPERATION=$1
CONFIG_JAVA_ORDER=$2
DEPLOY_BIN_DIR=$3
BUILD_DIR=$4
CURRENT_GSA_TYPE=$5
SG_LOOKUPGROUPS=$6
JVM_PROPERTIES=$7

 # set shell.
 TERM=xterm export TERM

 # setup env variables and common functions
cd ${DEPLOY_BIN_DIR}
 . set-deploy-env.sh


 EXT_JAVA_OPTIONS="${JVM_PROPERTIES}"; export EXT_JAVA_OPTIONS

 # setup service grid
 case ${OPERATION} in
   ${START_PROCESS})
      # setup JDK
      ${CONFIG_JAVA_SCRIPT} ${CONFIG_JAVA_ORDER}
      echo Setup JDK: ${JAVA_HOME}

      if [ "${CURRENT_GSA_TYPE}" == "1" ]; then
        LOOKUPGROUPS=${SG_LOOKUPGROUPS}; export LOOKUPGROUPS
      	${BUILD_DIR}/bin/gs-agent.sh gsa.global.lus=2 gsa.global.gsm=0 gsa.gsc=0 &
      fi
      if [ "${CURRENT_GSA_TYPE}" == "2" ]; then
        LOOKUPGROUPS=${SG_LOOKUPGROUPS}_WAN_`hostname`; export LOOKUPGROUPS
      	${BUILD_DIR}/bin/gs-agent.sh gsa.global.lus=2 gsa.global.gsm=0 gsa.gsc=0 &
      fi
   ;;
   ${KILL_PROCESS})
      echo Kill all [${TOTAL_RUNNING_AGENTS}] gs-agents...
      killall -9 java
   ;;
 esac