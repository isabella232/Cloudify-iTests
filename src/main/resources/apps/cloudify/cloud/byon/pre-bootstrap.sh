#! /bin/bash

# BYON machines may be dirty
echo checking for previous java installation
if [ -d "/tmp/byon/java" ]; then
	echo cleaning java installation from home directory
	rm -rf ~/java
fi

echo checking for previous gigaspaces installation
if [ -d "/tmp/byon/gigaspaces" ]; then
	echo cleaning gigaspaces installation from home directory
	rm -rf ~/gigaspaces
fi