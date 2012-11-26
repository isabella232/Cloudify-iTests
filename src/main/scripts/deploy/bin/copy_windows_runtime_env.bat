@echo setting up runtime variables

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
shift
shift
set BRANCH_NAME=%7
set SVN_BRANCH_DIRECTORY=%8
set EC2_REGION=%9

set SGTEST_RUNTIME_FOLDER=C:\Users\ca\sgtest-cloudify3.0


cd windows
@echo starting sgtest execution
@call startSG.bat %VERSION% %MILESTONE% %BUILD_NUMBER% %BUILD_VERSION% %SGTEST_CHECKOUT_FOLDER% %SUITE_NAME% %INCLUDE% %EXCLUDE% %BUILD_LOG_URL% %BRANCH_NAME% %SVN_BRANCH_DIRECTORY% %EC2_REGION%