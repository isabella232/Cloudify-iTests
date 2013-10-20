#!/bin/bash
#
# This script is a wrapper around the "gs" script, and provides the command line instruction
# to start the GigaSpaces Grid Service Container
# 		GSC_JAVA_OPTIONS 	- Extended java options that are proprietary defined  for GSC such as heap size, system properties or other JVM arguments that can be passed to the JVM command line. 
#							- These settings can be overridden externally to this script.

# sleep for rand number of seconds
rand=$RANDOM * 100 / 32768 + 1
sleep rand

services="com.gigaspaces.start.services=\"GSC\""

# GSC_JAVA_OPTIONS=; export GSC_JAVA_OPTIONS
COMPONENT_JAVA_OPTIONS="${GSC_JAVA_OPTIONS}"
export COMPONENT_JAVA_OPTIONS


`dirname $0`/gs.sh start $services $*
