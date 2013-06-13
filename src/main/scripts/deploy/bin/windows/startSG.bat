@rem This script is used to run our Selenium Web-UI tests on a windows machine.
@rem it uses the existing build.xml and run.xml of SGTest with configured run.properties.
@rem Note: this script is static

@title Executing Selenium tests

@echo on

set VERSION=%1
set MILESTONE=%2
set BUILD_NUMBER=%3
set BUILD_VERSION=%4
set SUITE_NAME=%5
set INCLUDE=%6
set EXCLUDE=%7
set BUILD_LOG_URL=%8
set BRANCH_NAME=%9

shift
set SVN_BRANCH_DIRECTORY=%9

shift
set EC2_REGION=%9

shift
set REVERSE_PROXY=%9

shift
set SUITE_TYPE=%9

shift
set MAVEN_PROJECTS_VERSION_XAP=%9

shift
set MAVEN_PROJECTS_VERSION_CLOUDIFY=%9

set BUILD_TEST_DIR=C:\%BUILD_NUMBER%
set SGTEST_HOME=%BUILD_TEST_DIR%\Cloudify-iTests

@echo setting up enviroment variables
call %SGTEST_HOME%\src\main\scripts\deploy\bin\windows\set-build-env.bat

@echo retrieving build from tarzan...
@mkdir %BUILD_LOCATION%
xcopy %REMOTE_BUILD_DIR%\cloudify\1.5\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip %BUILD_TEST_DIR%
@echo extracting build file to local-builds folder
@7z x %BUILD_TEST_DIR%\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip -o%BUILD_TEST_DIR%
@del %BUILD_TEST_DIR%\gigaspaces-cloudify-%VERSION%-%MILESTONE%-b%BUILD_VERSION%.zip

@call %SGTEST_HOME%\src\main\scripts\deploy\bin\windows\set-deploy-env.bat

@echo updating webuitf...
call %SGTEST_HOME%\src\main\scripts\deploy\bin\windows\deploy_webuitf.bat

@echo updating testing framework...
call %SGTEST_HOME%\src\main\scripts\deploy\bin\windows\deploy_framework.bat

@echo Running Suite %SUITE_NAME%: 
call %SGTEST_HOME%\src\main\scripts\deploy\bin\windows\start-suite.bat %SUITE_NAME% %INCLUDE% %EXCLUDE% %EC2_REGION% %BUILD_TEST_DIR% %REVERSE_PROXY% %SUITE_TYPE% %MAVEN_PROJECTS_VERSION_XAP% %MAVEN_PROJECTS_VERSION_CLOUDIFY%

@echo generating report...
call %SGTEST_HOME%\src\main\scripts\deploy\bin\windows\generate-report.bat %BUILD_NUMBER% %SUITE_NAME% %VERSION% %MILESTONE% %BUILD_LOG_URL% %SUITE_TYPE% %MAVEN_PROJECTS_VERSION_XAP% %MAVEN_PROJECTS_VERSION_CLOUDIFY%

if %selenium.browser% == Chrome (
	taskkill /im chromedriver.exe /F
	taskkill /im chrome.exe /F
)
if %selenium.browser% == Firefox (
	taskkill /im firefox.exe /F
)

@echo transferring reports to tgrid from %BUILD_TEST_DIR%
xcopy %BUILD_LOCATION% W:\%BUILD_NUMBER%\%BUILD_FOLDER% /s /i /y
xcopy %BUILD_TEST_DIR%\%SUITE_NAME% W:\%BUILD_NUMBER%\%SUITE_NAME% /s /i /y