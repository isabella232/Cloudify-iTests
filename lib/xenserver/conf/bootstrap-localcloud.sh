#! /bin/bash

#############################################################################
# Parameters:
# $1 - The IP of this server (Useful if multiple NICs exist)
#	$2 - not used
#	$3 - LOOKUPLOCATORS (recommended)
#	$4 - LOOKUPGROUPS (mandatory if LOOKUPLOCATORS is not specified)
# $5 - GSA ZONE (optional)
#############################################################################

# UPDATE SETENV SCRIPT...DO NOT SET LOOKUPLOCATOR AND LOOKUPGROUPS
/opt/setup-env.sh $1

if [ ! -z "$5" ]; then
  export GSA_JAVA_OPTIONS=-Dcom.gs.zones=$5
fi

cd /opt/gigaspaces/tools/cli/
nohup ./cloudify.sh bootstrap-localcloud --verbose > /dev/null &

exit 0