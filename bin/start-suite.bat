@echo running tests...
set SUITE_ID=0
set LOOKUPGROUPS=sgwebui-cloudify%SUITE_ID%
pause
call ant -buildfile %SGTEST_CHECKOUT_FOLDER%\bin\pre-run.xml
pause
call ant -buildfile %SGTEST_CHECKOUT_FOLDER%\bin\run.xml testsummary -DBUILD_NUMBER=%BUILD_NUMBER% -DSUITE_NAME=%SUITE_NAME% -DBUILD_DIR=%RUNTIME_BUILD_LOCATION% -DMAJOR_VERSION=%VERSION% -DMINOR_VERSION=%MILESTONE% -DSELENIUM_BROWSER=%selenium.browser% -DSUITE_ID=%SUITE_ID% -DSUITE_NUMBER=1
pause
call %SGTEST_CHECKOUT_FOLDER%\deploy\bin\webui\generate-report.bat %BUILD_NUMBER% %SUITE_NAME% %VERSION% %MILESTONE%