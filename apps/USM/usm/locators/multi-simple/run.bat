
start /B java -Dcom.sun.management.jmxremote.port=9988 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -jar simplejavaprocess.jar -port 7777
start /B java -Dcom.sun.management.jmxremote.port=9989 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -jar simplejavaprocess.jar -port 7778