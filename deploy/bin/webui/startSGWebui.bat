@rem This script is used to run our Selenium Web-UI tests on a windows machine.
@rem it uses the existing build.xml and run.xml of SGTest with configured run.properties.
@title Executing Selenium tests
@echo on

set USER_HOME=C:\Users\ca
set VERSION=%1
set MILESTONE=%2
set BUILD_NUMBER=%3
set BUILD_VERSION=%4
set SGTEST_CHECKOUT_FOLDER=%5
set LOCAL_SGPATH=%USER_HOME%\sgwebui-cloudify
set REMOTE_BUILD_DIR=\\tarzan\builds\%VERSION%\%BUILD_NUMBER%
set BASE_DIR=C:\Users\ca
set PRODUCT=cloudify

@echo cleaning sgtest...
@if exist %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER% rmdir %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER% /s /q

@echo retrieving build from tarzan...
xcopy %REMOTE_BUILD_DIR%\cloudify\1.5\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip %BASE_DIR%

@set BUILD_FOLDER=gigaspaces-cloudify-%VERSION%-%MILESTONE%

@call %LOCAL_SGPATH%\deploy\bin\webui\create_sgtest_jar.bat %SGTEST_CHECKOUT_FOLDER% %BUILD_FOLDER%

call ant -buildfile %LOCAL_SGPATH%\bin\run.xml relocate-build -DBUILD_VERSION=%BUILD_VERSION% -DBUILD_NUMBER=%BUILD_NUMBER% -Dbuild.folder=%BUILD_FOLDER%
@del %USER_HOME%\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip

@start %LOCAL_SGPATH%\deploy\bin\webui\start-agents.bat %BUILD_NUMBER% %BUILD_FOLDER%

@call %LOCAL_SGPATH%\deploy\bin\webui\start-suite.bat %BUILD_NUMBER% webui-Firefox %BUILD_FOLDER%

@echo shutting down agents
@call %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER%\gigaspaces\tools\groovy\bin\groovy.bat %LOCAL_SGPATH%\src\test\webui\resources\scripts\shutdown

@echo cleaning remote build folder
@call %LOCAL_SGPATH%\src\test\webui\resources\psexec.exe \\pc-lab73 -u GSPACES\ca -p password -c -f %LOCAL_SGPATH%\src\test\webui\resources\scripts\clean-xap.bat %VERSION% %MILESTONE%
@echo cleaning local build folder
@rmdir %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER%\gigaspaces /s /q

@echo tranferring reports to tgrid
xcopy %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER% \\tarzan\tgrid\sgtest-cloudify\deploy\local-builds\%BUILD_NUMBER% /s /i