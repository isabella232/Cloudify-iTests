@echo on

set SGTEST_CHECKOUT_FOLDER=%1
set BUILD_FOLDER=%2
set USER_HOME=C:\Users\ca
set LOCAL_SGPATH=%USER_HOME%\sgwebui-cloudify

@rem - Replace default configuration files with local-machine specific files.

@del %SGTEST_CHECKOUT_FOLDER%\sgtest_build.xml
@del %SGTEST_CHECKOUT_FOLDER%\build.xml
@copy %SGTEST_CHECKOUT_FOLDER%\src\test\webui\resources\scripts\build.xml %SGTEST_CHECKOUT_FOLDER%

@echo building sgtest.jar...
call ant -buildfile %LOCAL_SGPATH%\build.xml jar1_6 -DBUILD_VERSION=%BUILD_VERSION% -DBUILD_NUMBER=%BUILD_NUMBER% -Dbuild.folder=%BUILD_FOLDER% -DgsHome.dir=%USER_HOME%\%BUILD_FOLDER%
@echo moving jar to lib folder in local sgtest folder
@move %SGTEST_CHECKOUT_FOLDER%\output\gs-sgtest.jar %LOCAL_SGPATH%\lib