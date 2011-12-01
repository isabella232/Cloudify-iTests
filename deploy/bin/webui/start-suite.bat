set SUITE_NAME=%1

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
@set LOOKUPGROUPS=sgwebui-xap
call ant -buildfile %LOCAL_SGPATH%\bin\run.xml testsummary -DBUILD_NUMBER=%BUILD_NUMBER% -DSUITE_NAME=%SUITE_NAME% -DBUILD_DIR=%RUNTIME_BUILD_LOCATION% -DSELENIUM_BROWSER=%selenium.browser%
