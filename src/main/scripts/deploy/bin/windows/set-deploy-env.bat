set USER_HOME=C:\Users\ca
set apache.port=8000
set apache.home=%USER_HOME%\Apache Software Foundation\Apache2.2
set RUNTIME_BUILD_LOCATION=%BUILD_LOCATION%

@echo copying logging file to build

del %BUILD_LOCATION%\config\gs_logging.properties
xcopy %SGTEST_HOME%\src\main\config\gs_logging.properties %BUILD_LOCATION%\config