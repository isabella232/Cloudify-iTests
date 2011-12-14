set VERSION=%1
set MILESTONE=%2
set BUILD_VERSION=%3
set BUILD_NUMBER=%4
set USER_HOME=C:\Users\ca
set BUILD_FOLDER=gigaspaces-cloudify-%VERSION%-%MILESTONE%
set RUNTIME_BUILD_LOCATION=%USER_HOME%\%BUILD_FOLDER%
set REMOTE_BUILD_DIR=\\tarzan\builds\cloudify\%VERSION%\%BUILD_NUMBER%
set REMOTE_BUILD_FILE=%REMOTE_BUILD_DIR%\cloudify\1.5\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip
@echo retrieving build from tarzan...
xcopy %REMOTE_BUILD_FILE% %USER_HOME%
@echo extracting build file to local-builds folder
7z x %USER_HOME%\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip -o%USER_HOME%
@del %USER_HOME%\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip
xcopy %USER_HOME%\hsqldb.xml %RUNTIME_BUILD_LOCATION%\config\gsa

@echo starting agent
@set LOOKUPGROUPS=sgwebui-cloudify
@set BUILD_FOLDER=gigaspaces-cloudify-%VERSION%-%MILESTONE%
@start %RUNTIME_BUILD_LOCATION%\bin\gs-agent.bat gsa.global.lus 2 gsa.global.gsm 0 gsa.gsc 0