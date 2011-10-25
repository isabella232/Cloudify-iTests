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

exit 0
