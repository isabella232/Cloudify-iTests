#! /bin/bash

export EXT_JAVA_OPTIONS="-Xmx1024m -Xms1024m"


mkdir /opt/gigaspaces
unzip -q /opt/gigaspaces.zip -d /opt/gigaspaces
chmod -R 777 /opt/gigaspaces

mv /opt/gigaspaces/*/* /opt/gigaspaces	
mv /opt/lib/*.jar /opt/gigaspaces/lib/platform/esm
mv /opt/xenserver-machine-provisioning.jar /opt/gigaspaces/lib/platform/esm

# UPDATE SETENV SCRIPT...
echo Updating environment script
cd /opt/gigaspaces/bin

sed -i '1i export JAVA_HOME=/usr' setenv.sh

# DISABLE LINUX FIREWALL
service iptables save
service iptables stop
chkconfig iptables off

#ENABLE TCP KEEP ALIVE
echo 1  > /proc/sys/net/ipv4/tcp_keepalive_time
echo 1 > /proc/sys/net/ipv4/tcp_keepalive_intvl
echo 5  > /proc/sys/net/ipv4/tcp_keepalive_probes
echo 3000 > /proc/sys/net/core/netdev_max_backlog
echo 3000 > /proc/sys/net/core/somaxconn

exit 0
