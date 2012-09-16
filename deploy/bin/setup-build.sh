#!/bin/bash

# this script tries to find out wheter a requested build is already found in local-builds direcotry.
# if scirpt found requested build it will return after doing nothing, leaving calling script to work with found build.
# else (no such build), scirpt will look for requested build in ${REGRESSION_BUILDS_PATH}, download zip, unzip downloaded
# build and remove zip file.

 . set-build-env.sh

 # set local build directory with entered build number
 BUILD_CACHE_DIR=${BUILDS_CACHE_REPOSITORY}/${BUILD_NUMBER}

 IS_BUILD_EXIST=${BUILDS_CACHE_REPOSITORY}/${BUILD_NUMBER}
 if [ ! -d ${IS_BUILD_EXIST} ];
   then
	# start to download the build from the main build repository
	LOCAL_BUILD_NUMBER=`echo ${BUILD_NUMBER} | awk '{print substr($1,7)}'`
	BUILD_FILE=${BUILD_INSTALL_DIR}_${BUILD_JDK_VER}_b${LOCAL_BUILD_NUMBER}.zip

	if [ "${BUILD_JDK_VER}" = "1.5" ]
		then
		BUILD_FILE=${BUILD_INSTALL_DIR}-b${LOCAL_BUILD_NUMBER}.zip
		else
		BUILD_FILE=${BUILD_INSTALL_DIR}-${BUILD_JDK_VER}-b${LOCAL_BUILD_NUMBER}.zip
	fi

	# full path to the build.zip file
	if [ ${BUILD_TYPE} == "cloudify" ]; then
        	BUILD_PATH=${REGRESSION_BUILDS_PATH}/${BUILD_NUMBER}/${BUILD_TYPE}/${BUILD_JDK_VER}/${BUILD_FILE}
	else
        	BUILD_PATH=${REGRESSION_BUILDS_PATH}/${BUILD_NUMBER}/xap-bigdata/${BUILD_JDK_VER}/${BUILD_FILE}
	fi



	#check that build-file path is available and readable
	if [ ! -r ${BUILD_PATH} ]
		then
		echo "ERROR: Build file doesn't not exist: ${BUILD_PATH}"
		exit 1
	fi

	############ Download the supplied build number to the cache_directory ################

	echo Removing the old build directory if exists...
	rm -rf ${BUILD_CACHE_DIR}

	# creating local build directory:
	echo Creating local build directory of selected build: ${BUILD_CACHE_DIR}
	mkdir $BUILD_CACHE_DIR

	printf "\n *** Starting to download build:"
	echo ${BUILD_PATH}
	printf "\n *** Please wait, setup in progress..."
	sleep 2

	# copy the download file to cache dir repository
	cp ${BUILD_PATH} ${BUILD_CACHE_DIR}

	#introduce new variable: full path to build zip file
	FULL_PATH_TO_BUILD_ZIP_FILE=${BUILD_CACHE_DIR}/${BUILD_FILE}

	if [ ! -r  ${FULL_PATH_TO_BUILD_ZIP_FILE} ]
		then
		echo "ERROR: Failed to download $BUILD_FILE file. File does not exist or is not readable. *The build number might be wrong.*"
		exit 1
	fi
	############ End of download ###########

	#unzip build
	printf "\n *** Starting to unzip Build file: ${BUILD_FILE} ..."
	sleep 2
	unzip ${FULL_PATH_TO_BUILD_ZIP_FILE} -d ${BUILD_CACHE_DIR}

	#check if unzip successed
	if [ ! -d ${BUILD_CACHE_DIR}/${BUILD_INSTALL_DIR} ]
		then
		echo "ERROR: Failed to unzip ${BUILD_FILE}. *Zip file might be currupted or not readable*"
		exit 1
	fi

	#delete the original zip file
	printf "\n *** Deleting original Build file: ${BUILD_FILE} file..."
	sleep 2
	rm -rf ${FULL_PATH_TO_BUILD_ZIP_FILE}
 else
		# build exists in local-builds - don't download it, just use it.
		echo "using build already found in ${BUILDS_CACHE_REPOSITORY}"
 fi