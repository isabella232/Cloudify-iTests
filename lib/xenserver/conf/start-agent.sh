#! /bin/bash

#############################################################################
# Parameters:
# $1 - The IP of this server (Useful if multiple NICs exist)
#	$2 - 'agent' if this machine should start a lus/gsm/esm
#	$3 - LOOKUPLOCATORS (recommended)
#	$4 - LOOKUPGROUPS (mandatory if LOOKUPLOCATORS is not specified)
# $5 - GSA ZONE (optional)
#############################################################################

# UPDATE SETENV SCRIPT...
/opt/setup-env.sh $1 $3 $4

if [ -z "$5" ]; then
  export GSA_JAVA_OPTIONS=-Dcom.gs.agent.auto-shutdown-enabled=true
else
  export GSA_JAVA_OPTIONS="-Dcom.gs.agent.auto-shutdown-enabled=true -Dcom.gs.zones=$5"
fi

cd /opt/gigaspaces/bin
if [ "$2" = "agent" ]; then  
	nohup ./gs-agent.sh gsa.lus=1 gsa.global.lus=0 gsa.gsm=1 gsa.global.gsm=0 gsa.esm=1 gsa.gsc=0 > /dev/null &	
else
	nohup ./gs-agent.sh gsa.lus=0 gsa.global.lus=0 gsa.gsm=0 gsa.global.gsm=0 gsa.esm=0 gsa.gsc=0 > /dev/null &
fi	

exit 0
