#!/bin/bash

# 1 arg build-argument
# 2 arg major version.
# 3 arg minor version.
# 4 arg jdk version for XAP zip
# 5 arg jdk order version for config-java
# 6 arg client machine
# 7 arg machines list to run gs-agents on i.e: 12-17 to run the supplied id machines.
# 8 arg jvm properties

 #. startSG.sh build_2591-248 6.6.0 m2 1.5 17a 12 embedded '-Xms2g -Xmx2g' 'embedded.txt'
 #. startSG.sh build_2591-248 6.6.0 m2 1.5 17a 15 remote '-Xms2g -Xmx2g' 'remote.txt' 12 15
  . startSG.sh build_3494-534 7.0.0 m7 xap-premium 1.5 10 15 12,13 '-Xms2g -Xmx2g'
