set GS_HOME=D:\gigaspaces-cloudify-8.0.4-m3
set GS_AGENT_PATH=%GS_HOME%\bin\gs-agent.bat
set GS_AGENT_ARGUMENTS=gsa.gsc 0 gsa.lus 1 gsa.gsm 0 gsa.global.gsc 0 gsa.global.gsm 0 gsa.global.lus 0
set GS_AGENT_COMMAND=%GS_AGENT_PATH% %GS_AGENT_ARGUMENTS%


set ISREAL_GROUP=israel-%USERNAME%
set LONDON_GROUP=london-%USERNAME%
set NY_GROUP=ny-%USERNAME%

set LOOKUPGROUPS=%ISREAL_GROUP%
start %GS_AGENT_COMMAND%

set LOOKUPGROUPS=%LONDON_GROUP%
start %GS_AGENT_COMMAND%

set LOOKUPGROUPS=%NY_GROUP%
start %GS_AGENT_COMMAND%
