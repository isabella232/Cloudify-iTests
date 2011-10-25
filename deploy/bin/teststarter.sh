#!/bin/bash

umask 000

 echo "start time in `date`"
 echo ---------------------------------------
 echo ---------------------------------------

 ${PDSH} -w ssh:pc-lab[12] "remote.sh" 5400

 echo ---------------------------------------
 echo ---------------------------------------
 echo "stop time in `date`"
