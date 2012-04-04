@echo on

@start %LOCAL_SGPATH%\src\test\webui\resources\psexec.exe \\pc-lab73 -u GSPACES\ca -p password -c -f %LOCAL_SGPATH%\src\test\webui\resources\scripts\start-agent.bat %VERSION% %MILESTONE% %BUILD_VERSION% %BUILD_NUMBER%
@set LOOKUPGROUPS=sgwebui-cloudify
@start /b %RUNTIME_BUILD_LOCATION%\bin\gs-agent.bat gsa.global.lus 2 gsa.global.gsm 0 gsa.gsc 0