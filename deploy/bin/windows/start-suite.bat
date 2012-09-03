set SUITE_NAME=%1
set TEMP_INCLUDE=%2
set TEMP_EXCLUDE=%3
set INCLUDE=%TEMP_INCLUDE:_=,%
set EXCLUDE=%TEMP_EXCLUDE:_=,%

@cd %LOCAL_SGPATH%\bin

set selenium.browser=dummy
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
call ant -buildfile %LOCAL_SGPATH%\bin\pre-run.xml
call ant -buildfile %LOCAL_SGPATH%\bin\run-win.xml testsummary -DBUILD_NUMBER=%BUILD_NUMBER% -DSUITE_NAME=%SUITE_NAME% -DBUILD_DIR=%RUNTIME_BUILD_LOCATION% -DMAJOR_VERSION=%VERSION% -DMINOR_VERSION=%MILESTONE% -DSELENIUM_BROWSER=%selenium.browser% -DSUITE_ID=%SUITE_ID% -DSUITE_NUMBER=1 -DINCLUDE="%INCLUDE%" -DEXCLUDE="%EXCLUDE%"
call %LOCAL_SGPATH%\deploy\bin\windows\generate-report.bat %BUILD_NUMBER% %SUITE_NAME% %VERSION% %MILESTONE% %BUILD_LOG_URL%