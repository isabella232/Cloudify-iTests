@set VERSION=%1
@set MILESTONE=%2
@set BUILD_VERSION=%3
@set BUILD_NMBER=%4 
@set USER_HOME=C:\Users\ca
set REMOTE_BUILD_DIR=\\tarzan\builds\%VERSION%\%BUILD_NUMBER%

@echo retrieving build from tarzan...
xcopy %REMOTE_BUILD_DIR%\cloudify\1.5\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip %USER_HOME%
@extracting build file to local-builds folder
@7z e %USER_HOME%\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip %USER_HOME%
@del %USER_HOME%\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip

@echo starting agent
@set LOOKUPGROUPS=sgwebui-cloudify
@set BUILD_FOLDER=gigaspaces-cloudify-%VERSION%-%MILESTONE%
@start %USER_HOME%\%BUILD_FOLDER%\bin\gs-agent.bat gsa.global.lus 2 gsa.global.gsm 0 gsa.gsc 0