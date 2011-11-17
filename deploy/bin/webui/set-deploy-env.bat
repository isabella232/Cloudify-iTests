set USER_HOME=C:\Users\ca
set apache.port=8000
set apache.home=%USER_HOME%\Apache Software Foundation\Apache2.2
set LOCAL_SGPATH=%USER_HOME%\sgwebui-xap
set RUNTIME_BUILD_LOCATION=%USER_HOME%\%BUILD_FOLDER%

@echo copying build to user home dir, this is the build to be used in runtime
xcopy %BUILD_LOCATION% %RUNTIME_BUILD_LOCATION% /e /i