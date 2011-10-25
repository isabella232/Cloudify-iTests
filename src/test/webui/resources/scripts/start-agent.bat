@set REMOTE_BUILD_DIR=\\tarzan\users\ca\Eli_Test

@echo retrieving build from tarzan...
@mkdir C:\Users\ca\gigaspaces
@xcopy /e %REMOTE_BUILD_DIR%\gigaspaces C:\Users\ca\gigaspaces

@echo starting agent
@set LOOKUPGROUPS=sgtest-webui
@start C:\Users\ca\gigaspaces\bin\gs-agent.bat gsa.global.lus 2 gsa.global.gsm 0 gsa.gsc 0

@echo deleting build from tarzan\users\ca
@rmdir %REMOTE_BUILD_DIR%\gigaspaces /s /q
@taskkill /im conhost.exe /im cmd.exe /F