@set REMOTE_BUILD_DIR=\\tarzan\users\ca\Eli_Test

@set BUILD_FOLDER=%1
@set USER_HOME=%2

@echo retrieving build from tarzan...
@mkdir %USER_HOME%\%BUILD_FOLDER%
@xcopy /e %REMOTE_BUILD_DIR%\%BUILD_FOLDER% %USER_HOME%\%BUILD_FOLDER%

@echo starting agent
@set LOOKUPGROUPS=sgwebui-cloudify
@start %USER_HOME%\%BUILD_FOLDER%\bin\gs-agent.bat gsa.global.lus 2 gsa.global.gsm 0 gsa.gsc 0

@echo deleting build from tarzan\users\ca
@rmdir %REMOTE_BUILD_DIR%\%BUILD_FOLDER% /s /q
@taskkill /im conhost.exe /im cmd.exe /F