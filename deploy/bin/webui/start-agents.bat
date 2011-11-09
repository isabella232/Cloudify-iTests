@echo on

set BUILD_NUMBER=%1
set BUILD_FOLDER=%2
set USER_HOME=C:\Users\ca
set LOCAL_SGPATH=%USER_HOME%\sgwebui-cloudify

@echo starting agent on local machine
@set LOOKUPGROUPS=sgwebui-cloudify
@start %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER%\gigaspaces\bin\gs-agent.bat gsa.global.lus 2 gsa.global.gsm 0 gsa.gsc 0
@echo starting agent on remote macihne : pc-lab73
@start %LOCAL_SGPATH%\src\test\webui\resources\psexec.exe \\pc-lab73 -u GSPACES\ca -p password -c -f %LOCAL_SGPATH%\src\test\webui\resources\scripts\start-agent.bat %BUILD_FOLDER% %USER_HOME%
