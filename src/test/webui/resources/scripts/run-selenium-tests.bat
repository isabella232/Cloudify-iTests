@rem This script is used to run our Selenium Web-UI tests on a windows machine.
@rem it uses the existing build.xml and run.xml of SGTest with configured run.properties.
@title Executing Selenium tests
@echo on


@set svn-srv=192.168.9.34
@set SVN_REPOSITORY=svn://%svn-srv%/SVN
@set XAP_VERSION=%1
@set MILESTONE=%2
@set BUILD_NUMBER=%3
@set GROUP_NAME=%4
@set LOCAL_SGPATH=C:\Users\ca\sgtest-webui
@set REMOTE_BUILD_DIR=\\tarzan\builds\%XAP_VERSION%\build_%BUILD_NUMBER%
@set BASE_DIR=.\
@set apache.port=8000
@set apache.home=C:\Users\ca\Apache Software Foundation\Apache2.2
@set STOCK_DEMO_HOME=%LOCAL_SGPATH%\src\test\webui\resources\StockDemo
@set SVN_URL=%SVN_REPOSITORY%/trunk


if "%GROUP_NAME%" == "xap" (
    set PRODUCT=xap-premium
) else (
	set PRODUCT=cloudify
)

@set SVN_SGPATH=%SVN_URL%/quality/SGTest

@echo checking out SGTest from svn... 
svn checkout %SVN_SGPATH% %LOCAL_SGPATH%

@echo cleaning sgtest...
@if exist %BASE_DIR%\sgtest-webui\deploy\local-builds\build_%BUILD_NUMBER% rmdir %BASE_DIR%\sgtest-webui\deploy\local-builds\build_%BUILD_NUMBER% /s /q
@if exist \\tarzan\tgrid\sgtest.webui\index.htm del \\tarzan\tgrid\sgtest.webui\index.htm
@if exist \\tarzan\tgrid\sgtest.webui\build_%BUILD_NUMBER% rmdir \\tarzan\tgrid\sgtest.webui\build_%BUILD_NUMBER% /s /q

@echo cleaning remote host working dir
@start %LOCAL_SGPATH%\src\test\webui\resources\psexec.exe \\pc-lab73 -u GSPACES\ca -p password -c -f %LOCAL_SGPATH%\src\test\webui\resources\scripts\clean-xap.bat

@echo retrieving build from tarzan...
xcopy %REMOTE_BUILD_DIR%\%PRODUCT%\1.5\gigaspaces-%PRODUCT%-%XAP_VERSION%-%MILESTONE%-b%BUILD_NUMBER%.zip %BASE_DIR%

@set BUILD_FOLDER=gigaspaces-%PRODUCT%-%XAP_VERSION%-%MILESTONE%

@rem - Replace default configuration files with local-machine specific files.

@del %LOCAL_SGPATH%\sgtest_build.xml
@copy %LOCAL_SGPATH%\src\test\webui\resources\scripts\sgtest_build.xml %LOCAL_SGPATH%
@del %LOCAL_SGPATH%\build.xml
@copy %LOCAL_SGPATH%\src\test\webui\resources\scripts\build_%GROUP_NAME%.xml %LOCAL_SGPATH%
@del %LOCAL_SGPATH%\bin\run.xml
@copy %LOCAL_SGPATH%\src\test\webui\resources\scripts\run.xml %LOCAL_SGPATH%\bin
@del %LOCAL_SGPATH%\bin\run.properties
@copy %LOCAL_SGPATH%\src\test\webui\resources\scripts\run.properties %LOCAL_SGPATH%\bin

@echo building sgtest.jar...
call ant -buildfile %LOCAL_SGPATH%\build_%GROUP_NAME%.xml jar1_6 -DBUILD_NUMBER=%BUILD_NUMBER% -Dbuild.folder=%BUILD_FOLDER%
@move %LOCAL_SGPATH%\output\gs-sgtest.jar %LOCAL_SGPATH%\lib

call ant -buildfile %LOCAL_SGPATH%\bin\run.xml relocate-build -DBUILD_NUMBER=%BUILD_NUMBER% -Dbuild.folder=%BUILD_FOLDER%
@del C:\Users\ca\gigaspaces-%PRODUCT%-%XAP_VERSION%-%MILESTONE%-b%BUILD_NUMBER%.zip
@rmdir C:\Users\ca\gigaspaces /s /q

@cd %LOCAL_SGPATH%\bin

@echo starting agent on local machine
@set LOOKUPGROUPS=sgtest-webui
@start %LOCAL_SGPATH%\deploy\local-builds\build_%BUILD_NUMBER%\gigaspaces\bin\gs-agent.bat gsa.global.lus 2 gsa.global.gsm 0 gsa.gsc 0
@echo starting agent on remote macihne : pc-lab73
@start %LOCAL_SGPATH%\src\test\webui\resources\psexec.exe \\pc-lab73 -u GSPACES\ca -p password -c -f %LOCAL_SGPATH%\src\test\webui\resources\scripts\start-agent.bat

@set selenium.browser=Firefox
@set group.name=webui-Firefox-%GROUP_NAME%
@echo running Firefox tests...
call ant -buildfile run.xml testsummary -DBUILD_NUMBER=%BUILD_NUMBER% -Dgroup.name=%GROUP_NAME% -DSUITE_NAME=webui-Firefox-%GROUP_NAME% -Dbuild.folder=%BUILD_FOLDER% -Dgroup.name=%GROUP_NAME%
xcopy %LOCAL_SGPATH%\deploy\local-builds\build_%BUILD_NUMBER%\webui-Firefox-%GROUP_NAME% \\tarzan\tgrid\sgtest.webui\build_%BUILD_NUMBER%\webui-Firefox-%GROUP_NAME% /s /i

rem @set selenium.browser=Chrome
rem @set group.name=webui-Chrome-%GROUP_NAME%
rem @echo running Chrome tests...
rem call ant -buildfile run.xml testsummary -DBUILD_NUMBER=%BUILD_NUMBER% -Dgroup.name=%GROUP_NAME% -DSUITE_NAME=webui-Chrome-%GROUP_NAME% -Dbuild.folder=%BUILD_FOLDER% -Dgroup.name=%GROUP_NAME%
rem xcopy %LOCAL_SGPATH%\deploy\local-builds\build_%BUILD_NUMBER%\webui-Chrome-%GROUP_NAME% \\tarzan\tgrid\sgtest.webui\build_%BUILD_NUMBER%\webui-Chrome-%GROUP_NAME% /s /i

rem @set selenium.browser=IE
rem @echo running Internet Explorer tests...
rem call ant -buildfile run.xml testsummary -DBUILD_NUMBER=%BUILD_NUMBER% -Dgroup.name=%GROUP_NAME% -DSUITE_NAME=webui-IE-%GROUP_NAME% -Dbuild.folder=%BUILD_FOLDER% -Dgroup.name=%GROUP_NAME%
rem xcopy %LOCAL_SGPATH%\deploy\local-builds\build_%BUILD_NUMBER%\webui-IE-%GROUP_NAME% \\tarzan\tgrid\sgtest.webui\build_%BUILD_NUMBER%\webui-IE-%GROUP_NAME% /s /i

@echo tranfering reports to tgrid...
xcopy %LOCAL_SGPATH%\deploy\local-builds\index.htm \\tarzan\tgrid\sgtest.webui /y

@echo shutting down agent
@call %LOCAL_SGPATH%\deploy\local-builds\build_%BUILD_NUMBER%\gigaspaces\tools\groovy\bin\groovy.bat %LOCAL_SGPATH%\src\test\webui\resources\scripts\shutdown

@echo cleaning remote build folder
@call %LOCAL_SGPATH%\src\test\webui\resources\psexec.exe \\pc-lab73 -u GSPACES\ca -p password -c -f %LOCAL_SGPATH%\src\test\webui\resources\scripts\clean-xap.bat
@echo cleaning local build folder
@rmdir %LOCAL_SGPATH%\deploy\local-builds\build_%BUILD_NUMBER%\gigaspaces /s /q