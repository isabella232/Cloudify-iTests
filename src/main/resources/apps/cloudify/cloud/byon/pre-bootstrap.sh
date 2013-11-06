#! /bin/bash

USER=`whoami`

# BYON machines may be dirty
echo checking for previous java installation
if [ -d "/tmp/$USER/java" ]; then
	echo cleaning java installation from home directory
	rm -rf /tmp/$USER/java
fi

echo checking for previous gigaspaces installation
if [ -d "/tmp/$USER/gigaspaces" ]; then
	echo cleaning gigaspaces installation from home directory
	rm -rf /tmp/$USER/gigaspaces
fi