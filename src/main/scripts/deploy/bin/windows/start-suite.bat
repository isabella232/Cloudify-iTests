set SUITE_NAME=%1
set TEMP_INCLUDE=%2
set TEMP_EXCLUDE=%3
set INCLUDE=%TEMP_INCLUDE:_=,%
set EXCLUDE=%TEMP_EXCLUDE:_=,%
set EC2_REGION=%4
set LOC_BUILD_TEST_DIR=%5
set REVERSE_PROXY=%6
set SUITE_TYPE=%7
set MAVEN_PROJECTS_VERSION_XAP=%8
set MAVEN_PROJECTS_VERSION_CLOUDIFY=%9

shift
set ENABLE_LOGSTASH=%9

@cd %LOCAL_SGPATH%\bin

set selenium.browser=Firefox
if %SUITE_NAME% == Cloudify_Webui_Firefox (
	set selenium.browser=Firefox
)
if %SUITE_NAME% == Cloudify_Webui_Chrome (
	set selenium.browser=Chrome
)
if %SUITE_NAME% == Cloudify_Webui_IE (
	set selenium.browser=IE
)

set reverse.proxy=false
if %REVERSE_PROXY% == use-reverse-proxy (
	set reverse.proxy=true
)

@echo running %selenium.browser% tests...
set SUITE_ID=0

echo "making suite dir" %LOC_BUILD_TEST_DIR%\%SUITE_NAME%
mkdir %LOC_BUILD_TEST_DIR%\%SUITE_NAME%

pushd %SGTEST_HOME%

call mvn test -U -P tgrid-cloudify-iTests ^
-DiTests.cloud.enabled=false ^
-DiTests.buildNumber=${BUILD_NUMBER} ^
-DiTests.enableLogstash=${ENABLE_LOGSTASH} ^
-Dcloudify.home=%RUNTIME_BUILD_LOCATION% ^
-Dincludes="%INCLUDE%" ^
-Dexcludes="%EXCLUDE%" ^
-Dselenium.browser=%selenium.browser% ^
-Dreverse.proxy=%reverse.proxy% ^
-Djava.security.policy=policy/policy.all ^
-Djava.awt.headless=true ^
-DiTests.suiteName=%SUITE_NAME% ^
-DiTests.suiteId=%SUITE_ID% ^
-DiTests.summary.dir=%LOC_BUILD_TEST_DIR%\%SUITE_NAME% ^
-DiTests.numOfSuites=1 ^
-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger ^
-Dcom.gs.logging.level.config=true ^
-Djava.util.logging.config.file=%SGTEST_HOME%/src/main/config/sgtest_logging.properties ^
-Dsgtest.buildFolder=../ ^
-DiTests.url=http://192.168.9.121:8087/sgtest3.0-cloudify/ ^
-Dec2.region=%EC2_REGION% ^
-DiTests.suiteType=%SUITE_TYPE% ^
-DgsVersion=%MAVEN_PROJECTS_VERSION_XAP% ^
-DcloudifyVersion=%MAVEN_PROJECTS_VERSION_CLOUDIFY%

popd