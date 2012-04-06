@echo on

@rem - Replace default configuration files with local-machine specific files.

@echo building sgtest.jar...
call ant -buildfile %SGTEST_CHECKOUT_FOLDER%\build-win.xml jar1_6 -DgsHome.dir=%BUILD_LOCATION%
@echo moving jar to lib folder in local sgtest folder
move %SGTEST_CHECKOUT_FOLDER%\output\gs-sgtest.jar %LOCAL_SGPATH%\lib