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
set BRANCH_NAME=%9

set SGTEST_RUNTIME_FOLDER=C:\Users\ca\sgtest-cloudify

@echo copying execution script to runtime sgtest folder
@if exist %SGTEST_RUNTIME_FOLDER%\deploy\bin\windows rmdir %SGTEST_RUNTIME_FOLDER%\deploy\bin\windows /s /q
@xcopy %SGTEST_CHECKOUT_FOLDER%\deploy\bin\windows %SGTEST_RUNTIME_FOLDER%\deploy\bin\windows /s /i /y

@echo copying run.xml to runtime sgtest folder

@if exist %SGTEST_RUNTIME_FOLDER%\bin\run-win.xml del %SGTEST_RUNTIME_FOLDER%\bin\run-win.xml /s /q
@xcopy %SGTEST_CHECKOUT_FOLDER%\bin\run-win.xml %SGTEST_RUNTIME_FOLDER%\bin /s /i /y

@if exist %SGTEST_RUNTIME_FOLDER%\bin\pre-run.xml del %SGTEST_RUNTIME_FOLDER%\bin\pre-run.xml /s /q
@xcopy %SGTEST_CHECKOUT_FOLDER%\bin\pre-run.xml %SGTEST_RUNTIME_FOLDER%\bin /s /i /y

@if exist %SGTEST_RUNTIME_FOLDER%\bin\post-run.xml del %SGTEST_RUNTIME_FOLDER%\bin\post-run.xml /s /q
@xcopy %SGTEST_CHECKOUT_FOLDER%\bin\post-run.xml %SGTEST_RUNTIME_FOLDER%\bin /s /i /y

@if exist %SGTEST_RUNTIME_FOLDER%\config\gs_logging.properties del %SGTEST_RUNTIME_FOLDER%\config\gs_logging.properties /s /q
@xcopy %SGTEST_CHECKOUT_FOLDER%\config\gs_logging.properties %SGTEST_RUNTIME_FOLDER%\config /s /i /y

@echo copying test resources to sgtest runtime folder

@if exist %SGTEST_RUNTIME_FOLDER%\src\test\webui\resources rmdir %SGTEST_RUNTIME_FOLDER%\src\test\webui\resources /s /q
@xcopy %SGTEST_CHECKOUT_FOLDER%\src\test\webui\resources %SGTEST_RUNTIME_FOLDER%\src\test\webui\resources /s /i /y

@echo copying config files to sgtest runtime folder
@if exist %SGTEST_RUNTIME_FOLDER%\config rmdir %SGTEST_RUNTIME_FOLDER%\config /s /q
@xcopy %SGTEST_CHECKOUT_FOLDER%\config %SGTEST_RUNTIME_FOLDER%\config /s /i /y


cd windows
@echo starting sgtest execution
@call startSG.bat %VERSION% %MILESTONE% %BUILD_NUMBER% %BUILD_VERSION% %SGTEST_CHECKOUT_FOLDER% %SUITE_NAME% %INCLUDE% %EXCLUDE% %BUILD_LOG_URL% %BRANCH_NAME%