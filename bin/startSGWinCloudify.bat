@rem This script is used to run our Cloudify SGTest on a windows machine.
@rem it uses the existing build.xml and run.xml of SGTest.
@title Executing Cloudify SGTest tests
@echo on

@set VERSION=%1
@set MILESTONE=%2
@set BUILD_NUMBER=%3
@set BUILD_VERSION=%4
@set SGTEST_CHECKOUT_FOLDER=%5

REM set VERSION=2.1.0
REM set MILESTONE=m4
REM set BUILD_VERSION=1194-214
REM set BUILD_NUMBER=build_%BUILD_VERSION%
REM set SGTEST_CHECKOUT_FOLDER=D:\cloudify-workspace\SGTest

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

@echo copying build to user home dir, this is the build to be used in runtime
xcopy %BUILD_LOCATION% %RUNTIME_BUILD_LOCATION% /e /i /q /y /s
xcopy %LOCAL_SGPATH%\hsqldb.xml %RUNTIME_BUILD_LOCATION%\config\gsa /q /y

@echo creating sgtest.jar...
@call %SGTEST_CHECKOUT_FOLDER%\bin\create_sgtest_jar.bat

@call %SGTEST_CHECKOUT_FOLDER%\bin\download_processing_units.bat

@echo Running Test Suite : 
@call %SGTEST_CHECKOUT_FOLDER%\bin\start-suite.bat 

pause

@echo tranferring reports to tgrid
echo %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER%
xcopy %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER% Y:\%BUILD_NUMBER% /s /i /y
xcopy %LOCAL_SGPATH%\deploy\local-builds\wiki-summary\*.wiki Y:\wiki-summary /s /i /y
xcopy %LOCAL_SGPATH%\deploy\local-builds\wiki-backup\*.wiki Y:\wiki-backup /s /i /y

@pause

@echo cleaning local build folder
@rmdir %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER% /s /q
@rmdir %USER_HOME%\%BUILD_FOLDER% /s /q
