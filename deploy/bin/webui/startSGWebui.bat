@rem This script is used to run our Selenium Web-UI tests on a windows machine.
@rem it uses the existing build.xml and run.xml of SGTest with configured run.properties.
@title Executing Selenium tests
@echo on

set VERSION=%1
set MILESTONE=%2
set BUILD_NUMBER=%3
set BUILD_VERSION=%4
set SGTEST_CHECKOUT_FOLDER=%5
set selenium.browser=%6


@echo setting up enviroment variables
call set-build-env.bat

@echo cleaning sgtest...
@if exist %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER% rmdir %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER% /s /q

@echo retrieving build from tarzan...
@mkdir %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER%
xcopy %REMOTE_BUILD_DIR%\cloudify\1.5\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER%
@echo extracting build file to local-builds folder
7z x %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER%\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip -o%LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER%
@del %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER%\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip

@call set-deploy-env.bat

@echo creating sgtest.jar...
@call %LOCAL_SGPATH%\deploy\bin\webui\create_sgtest_jar.bat

@call %LOCAL_SGPATH%\deploy\bin\webui\download_processing_units.bat

@echo Running %selenium.browser% Suite : 
@call %LOCAL_SGPATH%\deploy\bin\webui\start-suite.bat webui-%selenium.browser%

@echo tranferring reports to tgrid
echo %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER%
xcopy %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER% Y:\%BUILD_NUMBER% /s /i /y
xcopy %LOCAL_SGPATH%\deploy\local-builds\wiki-summary\*.wiki Y:\wiki-summary /s /i /y
xcopy %LOCAL_SGPATH%\deploy\local-builds\wiki-backup\*.wiki Y:\wiki-backup /s /i /y

@echo cleaning local build folder
@rmdir %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER% /s /q
@rmdir %USER_HOME%\%BUILD_FOLDER% /s /q
