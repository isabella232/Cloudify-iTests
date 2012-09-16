#!/bin/bash

##
# Functions used by automated benchmark deploy:
# --------------------------------------------
# 1) clean_machines - clean all target machines of java processes.
# 2) get_target_machines_array - create an array of target machines without duplicate machines.
# 3) run_gs-agents - run gs-agents.sh on requested machines.
#
# @author Kobi
# @Date	  7-April-2009
##

 #this function will try to clean java processes on target machines.
 clean_machines()
 {
	 echo "> Clean target pc-lab machines"
	 if [ "$TARGET_GSA_MACHINES" != "" -a  "$TARGET_GSA_MACHINES" != "dummy" ]
		then
		. ${DEPLOY_ROOT_BIN_DIR}/preRemoteProcess.sh 2 ${TARGET_GSA_MACHINES}
	 fi
	 if [ "$TARGET_CLIENT_MACHINE" != ""  -a  "$TARGET_CLIENT_MACHINE" != "dummy" ]
		then
		. ${DEPLOY_ROOT_BIN_DIR}/preRemoteProcess.sh 2 ${TARGET_CLIENT_MACHINE}
	 fi
	if [ "$TARGET_GSA_WAN_MACHINES" != ""  -a  "$TARGET_GSA_WAN_MACHINES" != "dummy" ]
                then
                . ${DEPLOY_ROOT_BIN_DIR}/preRemoteProcess.sh 2 ${TARGET_GSA_WAN_MACHINES}
         fi		
		#{TARGET_GSA_WAN_MACHINES
		#TARGET_WAN_MACHINES_ARRAY 						  

 }

 #create an array of all participating machines in the execution.
 #this array ignores any duplicates targets found in gsa, client.
 get_target_machines_array()
 {
 	declare -a targets_array

 	#split line (tokenizers are comma, white spaces).
 	machines_str="${TARGET_GSA_MACHINES} ${TARGET_CLIENT_MACHINE}"
	targets_array=(`echo $machines_str | tr ',' ' '`)

	#sort array
	targets_array_sorted=( $(for ((i=0; i < "${#targets_array[@]}"; i++)); do echo ${targets_array[$i]}; done | sort) )

	#delete duplicates
	e=0
	result=${targets_array_sorted[0]}
	for ((s=1 ; s < ${#targets_array_sorted[@]} ; s++ )); do
		if [ ${targets_array_sorted[$e]} !=  ${targets_array_sorted[$s]} ]; then
	  		result="$result ${targets_array_sorted[$s]}"
	  		let 'e=s'
	  	fi
	done

	#assign value.
	TARGET_MACHINES_ARRAY=($result)
 }


 #this function run gs-agents.sh on requested machines.
 run_gsagents()
 {
	# a. start gs-agent process on requested machines.
	#-------------------------------------------
	if [ "$TARGET_GSA_MACHINES" != "" ]
	then
	echo "> Start all gs-agents on target pc-lab machines"
	./preRemoteProcess.sh 1 ${TARGET_GSA_MACHINES} ${GSA_TYPE} &
	fi

	if [ "$TARGET_GSA_WAN_MACHINES" != "" ]
	then
	echo "> Start all WAN gs-agents on target pc-lab machines"
	./preRemoteProcess.sh 1 ${TARGET_GSA_WAN_MACHINES} ${GSA_WAN_TYPE} &
	fi

	echo "build cache dir=${BUILD_CACHE_DIR}"
	chmod -R 777 ${BUILD_CACHE_DIR}

	sleep 30
 }