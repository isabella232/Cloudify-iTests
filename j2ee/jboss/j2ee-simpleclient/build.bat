@echo off

set JSHOMEDIR=d:\home\head\gigaspaces-xap-premium-8.0.0-m8
set LOOKUPGROUPS=dank-123
@call %JSHOMEDIR%\bin\setenv.bat

@%JAVACMD% %LOOKUP_GROUPS_PROP% -classpath %ANT_JARS%;"%JAVA_HOME%/lib/tools.jar" org.apache.tools.ant.Main %1
