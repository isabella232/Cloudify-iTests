set BUILD_NUMBER=%1
set SUITE_NAME=%2
set BUILD_FOLDER=%3
set USER_HOME=C:\Users\ca
set apache.port=8000
set apache.home=%USER_HOME%\Apache Software Foundation\Apache2.2
set LOCAL_SGPATH=%USER_HOME%\sgwebui-cloudify

@cd %LOCAL_SGPATH%\bin

if %SUITE_NAME% == "webui-Firefox" (
	set selenium.browser=Firefox
)
if %SUITE_NAME% == "webui-Chrome" (
	set selenium.browser=Chrome
)
if %SUITE_NAME% == "webui-IE" (
	set selenium.browser=IE
)
 
@echo running %selenium.browser% tests...
call ant -buildfile run.xml testsummary -DBUILD_NUMBER=%BUILD_NUMBER%  -DSUITE_NAME=$SUITE_NAME% -Dbuild.folder=%BUILD_FOLDER%
