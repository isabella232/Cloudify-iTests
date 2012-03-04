@echo setting up runtime variables

set VERSION=%1
set MILESTONE=%2
set BUILD_NUMBER=%3
set BUILD_VERSION=%4
set SGTEST_CHECKOUT_FOLDER=%5
set selenium.browser=%6
set SGTEST_RUNTIME_FOLDER=C:\Users\ca\sgwebui-cloudify

@echo copying execution script to runtime sgtest folder
@if exist %SGTEST_RUNTIME_FOLDER%\deploy\bin\webui rmdir %SGTEST_RUNTIME_FOLDER%\deploy\bin\webui /s /q
@xcopy %SGTEST_CHECKOUT_FOLDER%\deploy\bin\webui %SGTEST_RUNTIME_FOLDER%\deploy\bin\webui /s /i /y

@echo copying run.xml and run.properties to runtime sgtest folder

@if exist %SGTEST_RUNTIME_FOLDER%\bin\run.xml rmdir %SGTEST_RUNTIME_FOLDER%\bin\run.xml /s /q
@xcopy %SGTEST_CHECKOUT_FOLDER%\src\test\webui\resources\scripts\run.xml %SGTEST_RUNTIME_FOLDER%\bin /s /i /y

@if exist %SGTEST_RUNTIME_FOLDER%\bin\run.properties rmdir %SGTEST_RUNTIME_FOLDER%\bin\run.properties /s /q
@xcopy %SGTEST_CHECKOUT_FOLDER%\src\test\webui\resources\scripts\run.properties %SGTEST_RUNTIME_FOLDER%\bin /s /i /y

@echo copying test resources to sgtest runtime folder

@if exist %SGTEST_RUNTIME_FOLDER%\src\test\webui\resources rmdir %SGTEST_RUNTIME_FOLDER%\src\test\webui\resources /s /q
@xcopy %SGTEST_CHECKOUT_FOLDER%\src\test\webui\resources %SGTEST_RUNTIME_FOLDER%\src\test\webui\resources /s /i /y

@echo copying config files to sgtest runtime folder
@if exist %SGTEST_RUNTIME_FOLDER%\config rmdir %SGTEST_RUNTIME_FOLDER%\config /s /q
@xcopy %SGTEST_CHECKOUT_FOLDER%\config %SGTEST_RUNTIME_FOLDER%\config /s /i /y


cd webui
@echo starting sgtest execution
@call startSGWebui.bat %VERSION% %MILESTONE% %BUILD_NUMBER% %BUILD_VERSION% %SGTEST_CHECKOUT_FOLDER% %selenium.browser%