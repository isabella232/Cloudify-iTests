set SUITE_NAME=%1
set TEMP_INCLUDE=%2
set TEMP_EXCLUDE=%3
set INCLUDE=%TEMP_INCLUDE:_=,%
set EXCLUDE=%TEMP_EXCLUDE:_=,%
set EC2_REGION=%4

@cd %LOCAL_SGPATH%\bin

set selenium.browser=Firefox
if %SUITE_NAME% == webui-Firefox (
	set selenium.browser=Firefox
)
if %SUITE_NAME% == webui-Chrome (
	set selenium.browser=Chrome
)
if %SUITE_NAME% == webui-IE (
	set selenium.browser=IE
)
 
@echo running %selenium.browser% tests...
set SUITE_ID=0

pushd %SGTEST_HOME%
call mvn test -e -X -U ^
-Dcloudify.home=%RUNTIME_BUILD_LOCATION% ^
-Dincludes="%INCLUDE%" ^
-Dexcludes="%EXCLUDE%" ^
-Dselenium.browser=%selenium.browser% ^
-Djava.security.policy=policy/policy.all ^
-Djava.awt.headless=true ^
-Dsgtest.suiteName=%SUITE_NAME% ^
-Dsgtest.suiteId=%SUITE_ID% ^
-Dsgtest.summary.dir=%BUILD_DIR%/../%SUITE_NAME% ^
-Dsgtest.numOfSuites=1 ^
-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger ^
-Dcom.gs.logging.level.config=true ^
-Djava.util.logging.config.file=%SGTEST_HOME%/src/main/config/sgtest_logging.properties ^
-Dsgtest.buildFolder=../ ^
-Dsgtest.url=http://192.168.9.121:8087/sgtest3.0-cloudify/ ^
-Dec2.region=%EC2_REGION%

popd

call %SGTEST_HOME%\src\main\scripts\deploy\bin\windows\generate-report.bat %BUILD_NUMBER% %SUITE_NAME% %VERSION% %MILESTONE% %BUILD_LOG_URL%