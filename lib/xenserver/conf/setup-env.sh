#! /bin/bash

#############################################################################
# Parameters:
# $1 - The IP of this server (Useful if multiple NICs exist)
#	$2 - LOOKUPLOCATORS (recommended)
#	$3 - LOOKUPGROUPS (mandatory if LOOKUPLOCATORS is not specified)
#############################################################################

# UPDATE SETENV SCRIPT...
echo Updating environment script
cd /opt/gigaspaces/bin

sed -i "1i hostname localhost.localdomain" setenv.sh
sed -i "1i export NIC_ADDR=$1" setenv.sh
if [ ! -z "$2" ]; then
  sed -i "1i export LOOKUPLOCATORS=$2" setenv.sh
  # prevents components to register with other LUSs
  # shorter keep alive period for hard machine shutdown demo
  sed -i '1i export EXT_JAVA_OPTIONS="-Dcom.gs.multicast.enabled=false -Dcom.gs.transport_protocol.lrmi.connect_timeout=5s -Dnet.jini.lookup.JoinManager.maxLeaseDuration=15000"' setenv.sh
else
  # shorter keep alive period for hard machine shutdown demo
  sed -i '1i export EXT_JAVA_OPTIONS="-Dcom.gs.transport_protocol.lrmi.connect_timeout=5s -Dnet.jini.lookup.JoinManager.maxLeaseDuration=15000"' setenv.sh
fi


if [ ! -z "$3" ]; then
  # In case no locators defined, this setting prevents components to register with other lookup services
  # Also prevents other components to register with our Lookup service
  sed -i "1i export LOOKUPGROUPS=$3" setenv.sh
fi

echo Enabling TCP Keep Alive
echo 1  > /proc/sys/net/ipv4/tcp_keepalive_time
echo 1 > /proc/sys/net/ipv4/tcp_keepalive_intvl
echo 5  > /proc/sys/net/ipv4/tcp_keepalive_probes
echo 3000 > /proc/sys/net/core/netdev_max_backlog
echo 3000 > /proc/sys/net/core/somaxconn


exit 0