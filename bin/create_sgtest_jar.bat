@echo on

@rem - Replace default configuration files with local-machine specific files.

del %SGTEST_CHECKOUT_FOLDER%\build.xml
copy %SGTEST_CHECKOUT_FOLDER%\src\test\webui\resources\scripts\build.xml %SGTEST_CHECKOUT_FOLDER%

@echo building sgtest.jar...
call ant -buildfile %SGTEST_CHECKOUT_FOLDER%\build.xml jar1_6 -DgsHome.dir=%BUILD_LOCATION%
@echo moving jar to lib folder in local sgtest folder
xcopy %SGTEST_CHECKOUT_FOLDER%\output\gs-sgtest.jar %SGTEST_CHECKOUT_FOLDER%\lib\gs-sgtest.jar /e /i /q /y /s
del %SGTEST_CHECKOUT_FOLDER%\output\gs-sgtest.jar
