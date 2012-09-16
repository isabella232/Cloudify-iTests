#!/bin/bash

##
# This script provides setup enviroment of distributed TGrid Agents dagent.sh & remote-cmd-executor.sh.
# and contains all shared variables and functions.
# Important to keep all vars here to coordinate dagent.sh and remote-cmd-executor.sh with the same vars values.
#
# @see dagent.sh
# @see remote-cmd-executor.sh
# @author Kobi
##

 #setup vars
 PDSH="pdsh"
 CONFIG_JAVA_SCRIPT=". /export/utils/bin/configJava.sh"
 DEPLOY_ROOT_BIN_DIR=`pwd`
 SGTEST_ROOT_DIR=${DEPLOY_ROOT_BIN_DIR}/../..
 REMOTE_EXECUTOR_SCRIPT="${DEPLOY_ROOT_BIN_DIR}/remote-sg-executor.sh"
 RESULT_INDICATOR_FILE="${DEPLOY_ROOT_BIN_DIR}/../result_indicator/sgtest_end"

 # all agents operation types
 START_PROCESS=1
 KILL_PROCESS=2

 #default operations
 DEFAULT_OPER_TYPE_AGENT=1
 DEFAULT_REMOTE_MACHINES=12-13

 # type of process to invoke. options are gs-agent.
 GSA_TYPE=1
 GSA_WAN_TYPE=2

 # SGTEST Type values
 BACKWARDS_SGTEST_TYPE=BACKWARDS
 REGULAR_SGTEST_TYPE=REGULAR

 # SGTEST Client Type values
 SGTEST_OLD_CLIENT_TYPE=OLD_CLIENT
 SGTEST_NEW_CLIENT_TYPE=NEW_CLIENT