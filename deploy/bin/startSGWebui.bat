@rem This script is used to run our Selenium Web-UI tests on a windows machine.
@rem it uses the existing build.xml and run.xml of SGTest with configured run.properties.
@title Executing Selenium tests
@echo on

set USER_HOME=C:\Users\ca
set svn-srv=192.168.9.34
set SVN_REPOSITORY=svn://%svn-srv%/SVN
set VERSION=%1
set MILESTONE=%2
set BUILD_NUMBER=%3
set BUILD_VERSION=%4
set LOCAL_SGPATH=%USER_HOME%\sgwebui-cloudify
set REMOTE_BUILD_DIR=\\tarzan\builds\%VERSION%\%BUILD_NUMBER%
set BASE_DIR=.\
set apache.port=8000
set apache.home=%USER_HOME%\Apache Software Foundation\Apache2.2
set SVN_URL=%SVN_REPOSITORY%/cloudify/trunk
set PRODUCT=cloudify
set SVN_SGPATH=%SVN_URL%/quality/frameworks/SGTest

@echo checking out SGTest from svn... 
svn checkout %SVN_SGPATH% %LOCAL_SGPATH%

@echo cleaning sgtest...
@if exist %BASE_DIR%\sgwebui-cloudify\deploy\local-builds\%BUILD_NUMBER% rmdir %BASE_DIR%\sgwebui-cloudify\deploy\local-builds\%BUILD_NUMBER% /s /q
@if exist \\tarzan\tgrid\sgtest.webui\index.htm del \\tarzan\tgrid\sgtest.webui\index.htm
@if exist \\tarzan\tgrid\sgtest.webui\%BUILD_NUMBER% rmdir \\tarzan\tgrid\sgtest.webui\%BUILD_NUMBER% /s /q

@echo retrieving build from tarzan...
xcopy %REMOTE_BUILD_DIR%\cloudify\1.5\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip %BASE_DIR%

@set BUILD_FOLDER=gigaspaces-cloudify-%VERSION%-%MILESTONE%

@rem - Replace default configuration files with local-machine specific files.

@del %LOCAL_SGPATH%\sgtest_build.xml
@del %LOCAL_SGPATH%\build.xml
@copy %LOCAL_SGPATH%\src\test\webui\resources\scripts\build.xml %LOCAL_SGPATH%
@del %LOCAL_SGPATH%\bin\run.xml
@copy %LOCAL_SGPATH%\src\test\webui\resources\scripts\run.xml %LOCAL_SGPATH%\bin
@del %LOCAL_SGPATH%\bin\run.properties
@copy %LOCAL_SGPATH%\src\test\webui\resources\scripts\run.properties %LOCAL_SGPATH%\bin

@echo building sgtest.jar...
call ant -buildfile %LOCAL_SGPATH%\build.xml jar1_6 -DBUILD_VERSION=%BUILD_VERSION% -DBUILD_NUMBER=%BUILD_NUMBER% -Dbuild.folder=%BUILD_FOLDER% -DgsHome.dir=%USER_HOME%\%BUILD_FOLDER%
@move %LOCAL_SGPATH%\output\gs-sgtest.jar %LOCAL_SGPATH%\lib

call ant -buildfile %LOCAL_SGPATH%\bin\run.xml relocate-build -DBUILD_VERSION=%BUILD_VERSION% -DBUILD_NUMBER=%BUILD_NUMBER% -Dbuild.folder=%BUILD_FOLDER%
@del %USER_HOME%\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip

@cd %LOCAL_SGPATH%\bin

@echo starting agent on local machine
@set LOOKUPGROUPS=sgwebui-cloudify
@start %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER%\%BUILD_FOLDER%\bin\gs-agent.bat gsa.global.lus 2 gsa.global.gsm 0 gsa.gsc 0
@echo starting agent on remote macihne : pc-lab73
@start %LOCAL_SGPATH%\src\test\webui\resources\psexec.exe \\pc-lab73 -u GSPACES\ca -p password -c -f %LOCAL_SGPATH%\src\test\webui\resources\scripts\start-agent.bat %BUILD_FOLDER% %USER_HOME%

@set selenium.browser=Firefox
@echo running Firefox tests...
call ant -buildfile run.xml testsummary -DBUILD_NUMBER=%BUILD_NUMBER%  -DSUITE_NAME=webui-Firefox -Dbuild.folder=%BUILD_FOLDER%

rem @set selenium.browser=Chrome
rem @echo running Chrome tests...
rem call ant -buildfile run.xml testsummary -DBUILD_NUMBER=%BUILD_NUMBER%  -DSUITE_NAME=webui-Chrome -Dbuild.folder=%BUILD_FOLDER%

@echo tranferring reports to tgrid
xcopy %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER% \\tarzan\tgrid\sgtest.webui\%BUILD_NUMBER% /s /i

rem @set selenium.browser=IE
rem @echo running Internet Explorer tests...
rem call ant -buildfile run.xml testsummary -DBUILD_NUMBER=%BUILD_NUMBER%  -DSUITE_NAME=webui-IE -Dbuild.folder=%BUILD_FOLDER% 
rem xcopy %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER%\webui-IE \\tarzan\tgrid\sgtest.webui\%BUILD_NUMBER%\webui-IE /s /i

@echo tranfering reports to tgrid...
xcopy %LOCAL_SGPATH%\deploy\local-builds\index.htm \\tarzan\tgrid\sgtest.webui /y

@echo shutting down agents
@call %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER%\%BUILD_FOLDER%\tools\groovy\bin\groovy.bat %LOCAL_SGPATH%\src\test\webui\resources\scripts\shutdown

@echo cleaning remote build folder
@call %LOCAL_SGPATH%\src\test\webui\resources\psexec.exe \\pc-lab73 -u GSPACES\ca -p password -c -f %LOCAL_SGPATH%\src\test\webui\resources\scripts\clean-xap.bat %VERSION% %MILESTONE%
@echo cleaning local build folder
@rmdir %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER%\gigaspaces /s /q