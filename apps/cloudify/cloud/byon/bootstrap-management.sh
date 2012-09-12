#! /bin/bash

#############################################################################
# NOTE!! - This script differs from the original one by assuming that java is alerady installed on the machines.
# 		   It also user the $WORKING_HOME_DIRECTORY as the gigaspaces install dir instead of using '~/gigaspaces'
#
# This script starts a Gigaspaces agent for use with the Gigaspaces
# Cloudify. The agent will function as management depending on the value of $GSA_MODE
#
# Parameters the should be exported beforehand:
# 	$LUS_IP_ADDRESS - Ip of the head node that runs a LUS and ESM. May be my IP. (Required)
#   $GSA_MODE - 'agent' if this node should join an already running node. Otherwise, any value.
#	$NO_WEB_SERVICES - 'true' if web-services (rest, webui) should not be deployed (only if GSA_MODE != 'agent')
#   $MACHINE_IP_ADDRESS - The IP of this server (Useful if multiple NICs exist)
#	$MACHINE_ZONES - This is required if this is not a management machine
# 	$WORKING_HOME_DIRECTORY - This is where the files were copied to (cloudify installation, etc..)
#	$CLOUDIFY_LINK - If this url is found, it will be downloaded to $WORKING_HOME_DIRECTORY/gigaspaces.zip
#	$CLOUDIFY_OVERRIDES_LINK - If this url is found, it will be downloaded and unzipped into the same location as cloudify
#	$CLOUD_FILE - Location of the cloud configuration file. Only available in bootstrap of management machines.
#	$NO_WEB_SERVICES - If set to 'true', indicates that the rest and web-ui services should not be deployed in this machine.
#	$CLOUDIFY_CLOUD_IMAGE_ID - If set, indicates the image ID for this machine.
#	$CLOUDIFY_CLOUD_HARDWARE_ID - If set, indicates the hardware ID for this machine.
#	$PASSWORD - the machine password
#############################################################################

# args:
# $1 the error code of the last command (should be explicitly passed)
# $2 the message to print in case of an error
# 
# an error message is printed and the script exists with the provided error code
function error_exit {
	echo "$2 : error code: $1"
	exit ${1}
}

# args:
# $1 the error code of the last command (should be explicitly passed)
# $2 the message to print in case of an error 
# $3 the threshold to exit on
#
# if (last_error_code [$1]) >= (threshold [$3]) the provided message[$2] is printed and the script
# exists with the provided error code ($1)
function error_exit_on_level {
	if [ ${1} -ge ${3} ]; then
		error_exit ${1} ${2}
	fi
}
echo LUS_IP_ADDRESS = $LUS_IP_ADDRESS
echo WORKING_HOME_DIRECTORY = $WORKING_HOME_DIRECTORY

# dont use user home for gigaspaces installation in byon tests. to avoid any accedential deletion or corruption of files
GIGASPACES_INSTALL_DIRECTORY=$WORKING_HOME_DIRECTORY/gigaspaces

# This is where the installation of Java would go. but for testing purposes we skip this since java is already installed.  

echo Existing JAVA_HOME value -- $JAVA_HOME 

if [ ! -z "$CLOUDIFY_LINK" ]; then
	echo Downloading cloudify installation from $CLOUDIFY_LINK.tar.gz
	wget -q $CLOUDIFY_LINK.tar.gz -O $WORKING_HOME_DIRECTORY/gigaspaces.tar.gz || error_exit $? "Failed downloading cloudify installation"
fi

if [ ! -z "$CLOUDIFY_OVERRIDES_LINK" ]; then
	echo Downloading cloudify overrides from $CLOUDIFY_OVERRIDES_LINK.tar.gz
	wget -q $CLOUDIFY_OVERRIDES_LINK.tar.gz -O $WORKING_HOME_DIRECTORY/gigaspaces_overrides.tar.gz || error_exit $? "Failed downloading cloudify overrides"
fi

# Todo: Check this condition
if [ ! -d "$GIGASPACES_INSTALL_DIRECTORY" -o $WORKING_HOME_DIRECTORY/gigaspaces.tar.gz -nt $GIGASPACES_INSTALL_DIRECTORY ]; then
	rm -rf $GIGASPACES_INSTALL_DIRECTORY || error_exit $? "Failed removing old gigaspaces directory"
	mkdir $GIGASPACES_INSTALL_DIRECTORY || error_exit $? "Failed creating gigaspaces directory"
	
	# 2 is the error level threshold. 1 means only warnings
	# this is needed for testing purposes on zip files created on the windows platform 
	tar xfz $WORKING_HOME_DIRECTORY/gigaspaces.tar.gz -C $GIGASPACES_INSTALL_DIRECTORY || error_exit_on_level $? "Failed extracting cloudify installation" 2 

	# Todo: consider removing this line
	chmod -R 777 $GIGASPACES_INSTALL_DIRECTORY || error_exit $? "Failed changing permissions in cloudify installion"
	mv $GIGASPACES_INSTALL_DIRECTORY/*/* $GIGASPACES_INSTALL_DIRECTORY || error_exit $? "Failed moving cloudify installation"
	
	if [ ! -z "$CLOUDIFY_OVERRIDES_LINK" ]; then
		echo Copying overrides into cloudify distribution
		tar xfz $WORKING_HOME_DIRECTORY/gigaspaces_overrides.tar.gz -d $GIGASPACES_INSTALL_DIRECTORY || error_exit_on_level $? "Failed extracting cloudify overrides" 2 		
	fi
fi

# if an overrides directory exists, copy it into the cloudify distribution
if [ -d $WORKING_HOME_DIRECTORY/cloudify-overrides ]; then
	cp -rf $WORKING_HOME_DIRECTORY/cloudify-overrides/* $GIGASPACES_INSTALL_DIRECTORY
fi

# UPDATE SETENV SCRIPT...
echo Updating environment script
echo GIGASPACES_INSTALL_DIRECTORY=$GIGASPACES_INSTALL_DIRECTORY
cd $GIGASPACES_INSTALL_DIRECTORY/bin || error_exit $? "Failed changing directory to bin directory"

sed -i "1i export NIC_ADDR=$MACHINE_IP_ADDRESS" setenv.sh || error_exit $? "Failed updating setenv.sh"
sed -i "1i export LOOKUPLOCATORS=$LUS_IP_ADDRESS" setenv.sh || error_exit $? "Failed updating setenv.sh"
sed -i "1i export CLOUDIFY_CLOUD_IMAGE_ID=$CLOUDIFY_CLOUD_IMAGE_ID" setenv.sh || error_exit $? "Failed updating setenv.sh"
sed -i "1i export CLOUDIFY_CLOUD_HARDWARE_ID=$CLOUDIFY_CLOUD_HARDWARE_ID" setenv.sh || error_exit $? "Failed updating setenv.sh"
sed -i "1i export PATH=$JAVA_HOME/bin:$PATH" setenv.sh || error_exit $? "Failed updating setenv.sh"
sed -i "1i export JAVA_HOME=$JAVA_HOME" setenv.sh || error_exit $? "Failed updating setenv.sh"
sed -i "1i export EXT_JAVA_OPTIONS=-Dcom.gs.multicast.enabled=false" setenv.sh || error_exit $? "Failed updating setenv.sh"
sed -i "1i export VERBOSE=true" setenv.sh || error_exit $? "Failed updating setenv.sh"

cd $GIGASPACES_INSTALL_DIRECTORY/tools/cli || error_exit $? "Failed changing directory to cli directory"

chmod +x $GIGASPACES_INSTALL_DIRECTORY/bin/*.sh
chmod +x $GIGASPACES_INSTALL_DIRECTORY/tools/cli/*.sh

# START AGENT ALONE OR WITH MANAGEMENT
if [ -f nohup.out ]; then
  rm nohup.out
fi

if [ -f nohup.out ]; then
   error_exit 1 "Failed to remove nohup.out Probably used by another process"
fi

# Privileged mode handling

if [ "$CLOUDIFY_AGENT_ENV_PRIVILEGED" = "true" ]; then
	export CLOUDIFY_USER=`whoami`
	if [ "$CLOUDIFY_USER" = "root" ]; then
		# root is privileged by definition
		echo Running as root
	else
		sudo -n ls || error_exit_on_level $? "Current user is not a sudoer, or requires a password for sudo" 1
	fi
	if [ ! -f "/etc/sudoers" ]; then
		error_exit 101 "Could not find sudoers file at expected location (/etc/sudoers)"
	fi	
	echo Setting privileged mode
	sudo sed -i 's/^Defaults.*requiretty/#&/g' /etc/sudoers  || error_exit_on_level $? "Failed to edit sudoers file to disable requiretty directive" 1

fi

if [ ! -z "$CLOUDIFY_AGENT_ENV_INIT_COMMAND" ]; then
	echo Executing initialization command
	$CLOUDIFY_AGENT_ENV_INIT_COMMAND
fi

if [ "$GSA_MODE" = "agent" ]; then
	ERRMSG="Failed starting agent"
	nohup ./cloudify.sh start-agent -timeout 30 --verbose -zone $MACHINE_ZONES -auto-shutdown
else
	ERRMSG="Failed starting management services"
	if [ "$NO_WEB_SERVICES" = "true" ]; then
		nohup ./cloudify.sh start-management -no-web-services -no-management-space -timeout 30 --verbose -auto-shutdown -cloud-file $CLOUD_FILE
	else
		nohup ./cloudify.sh start-management -timeout 30 --verbose -auto-shutdown -cloud-file $CLOUD_FILE
	fi
fi	

RETVAL=$?
echo cat nohup.out
cat nohup.out
if [ $RETVAL -ne 0 ]; then
  error_exit $RETVAL $ERRMSG
fi
exit 0
