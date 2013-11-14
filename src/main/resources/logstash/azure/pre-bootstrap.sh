#! /bin/bash

HOST_NAME=`hostname`
echo -e "#! /bin/bash\n echo $PASSWORD" > $WORKING_HOME_DIRECTORY/password.sh
chmod +x $WORKING_HOME_DIRECTORY/password.sh
SUDO_ASKPASS=$WORKING_HOME_DIRECTORY/password.sh
export SUDO_ASKPASS

# add localhost mapping to /etc/hosts
echo "$MACHINE_IP_ADDRESS $HOST_NAME" | sudo -A tee -a /etc/hosts

# enable current user to run sudo without a password
USER=`whoami`
echo "$USER ALL=(ALL)   NOPASSWD :ALL" | sudo -A tee -a /etc/sudoers

# delete the password script
rm $WORKING_HOME_DIRECTORY/password.sh

echo Downloading logstash to ~/
cd ~/
wget https://s3-eu-west-1.amazonaws.com/gigaspaces-maven-repository-eu/net/logstash/1.2.2/logstash-1.2.2.jar --no-check-certificate

echo starting log shipper
echo java home: $JAVA_HOME

mkdir ~/logstash/logs
touch ~/logstash/logs/logstash-shipper-log.txt
$JAVA_HOME/bin/java -jar logstash-1.2.2.jar agent -f /tmp/byon/gs-files/upload/cloudify-overrides/config/logstash/logstash-shipper.conf -l ~/logstash/logs/logstash-shipper-log.txt&
