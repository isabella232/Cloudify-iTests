@rem This script is used to run our Selenium Web-UI tests on a windows machine.
@rem it uses the existing build.xml and run.xml of SGTest with configured run.properties.
@rem Note: this script is static

@title Executing Selenium tests

@echo on

set VERSION=%1
set MILESTONE=%2
set BUILD_NUMBER=%3
set BUILD_VERSION=%4
set SGTEST_CHECKOUT_FOLDER=%5
set SUITE_NAME=%6
set INCLUDE=%7
set EXCLUDE=%8
set BUILD_LOG_URL=%9

shift
set BRANCH_NAME=%9

shift
set SVN_BRANCH_DIRECTORY=%9

shift
set EC2_REGION=%9

@echo setting up enviroment variables
call set-build-env.bat

@mkdir %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER%
set SGTEST_HOME=%LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER%\SGTest

@echo cleaning build folder..
@if exist %BUILD_LOCATION% rmdir %BUILD_LOCATION% /s /q
@if exist %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER% rmdir %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER% /s /q

@echo retrieving build from tarzan...
@mkdir %BUILD_LOCATION%
xcopy %REMOTE_BUILD_DIR%\cloudify\1.5\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip %USER_HOME%
@echo extracting build file to local-builds folder
@7z x %USER_HOME%\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip -o%USER_HOME%
@del %USER_HOME%\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip


@echo exporting SGTest
@if %BRANCH_NAME%==trunk (
	set SVN_SGTEST_REPOSITORY=svn://svn-srv/SVN/cloudify/trunk/quality/frameworks/SGTest
) else ( 
	set SVN_SGTEST_REPOSITORY=svn://svn-srv/SVN/cloudify/branches/%SVN_BRANCH_DIRECTORY%/%BRANCH_NAME%/quality/frameworks/SGTest
)

@mkdir %WEBUI_TMP_DIR%
svn export --force %SVN_SGTEST_REPOSITORY% %SGTEST_HOME% 

call mvn scm:export -DconnectionUrl=scm:svn:svn://svn-srv/SVN/cloudify/trunk/quality/frameworks/SGTest-credentials -DexportDirectory=${BUILD_DIR}/../SGTest/src/main/resources/credentials

@call %SGTEST_HOME%\src\main\scripts\deploy\bin\windows\set-deploy-env.bat

@echo updating webuitf...
call %SGTEST_HOME%\src\main\scripts\deploy\bin\windows\deploy_webuitf.bat

@echo Running Suite %SUITE_NAME%: 
call %SGTEST_HOME%\src\main\scripts\deploy\bin\windows\start-suite.bat %SUITE_NAME% %INCLUDE% %EXCLUDE% %EC2_REGION% %BUILD_TEST_DIR%

@echo generating report... 
call %SGTEST_HOME%\src\main\scripts\deploy\bin\windows\generate-report.bat %BUILD_NUMBER% %SUITE_NAME% %VERSION% %MILESTONE% %BUILD_LOG_URL%

if %selenium.browser% == Chrome (
	taskkill /im chromedriver.exe /F
	taskkill /im chrome.exe /F
)

if %selenium.browser% == Firefox (
	taskkill /im firefox.exe /F
)

@echo transferring reports to tgrid
echo %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER%
xcopy %BUILD_LOCATION% X:\%BUILD_NUMBER%\%BUILD_FOLDER% /s /i /y
xcopy %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER%\%SUITE_NAME% X:\%BUILD_NUMBER%\%SUITE_NAME% /s /i /y

@echo not cleaning local build folder
@rem rmdir %LOCAL_SGPATH%\deploy\local-builds\%BUILD_NUMBER% /s /q

@if exist %USER_HOME%\gigaspaces-cloudify-%VERSION%-%MILESTONE% (
    rmdir %USER_HOME%\gigaspaces-cloudify-%VERSION%-%MILESTONE% /s /q
)