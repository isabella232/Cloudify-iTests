@echo on

set SUITE_TYPE=Regression
set BUILD_NUMBER=%1
set SUITE_NAME=%2
set MAJOR_VERSION=%3
set MINOR_VERSION=%4
set BUILD_LOG_URL=%5


pushd %SGTEST_HOME%

set CLOUDIFY_HOME=%SGTEST_HOME%\..\gigaspaces-cloudify-%MAJOR_VERSION%-%MINOR_VERSION%

call mvn exec:java -Dexec.mainClass="framework.testng.report.TestsReportMerger" -Dexec.args="%SUITE_TYPE% %BUILD_NUMBER% %SUITE_NAME% %MAJOR_VERSION% %MINOR_VERSION%" -Dcloudify.home=%CLOUDIFY_HOME%

call mvn exec:java -Dexec.mainClass="framework.testng.report.wiki.WikiReporter" -Dexec.args="%SUITE_TYPE% %BUILD_NUMBER% %SUITE_NAME% %MAJOR_VERSION% %MINOR_VERSION% %BUILD_LOG_URL%" -Dcloudify.home=%CLOUDIFY_HOME%

popd
