@echo on

@rem - Replace default configuration files with local-machine specific files.

set WEBUI_TMP_DIR=%SGTEST_CHECKOUT_FOLDER%\apps\webuitf

@echo exporting webuitf

@if %BRANCH_NAME%==trunk (
	set SVN_WEBUITF_REPOSITORY=svn://pc-lab14/SVN/xap/trunk/quality/frameworks/webuitf
) else ( 
	set SVN_WEBUITF_REPOSITORY=svn://pc-lab14/SVN/xap/branches/%SVN_BRANCH_DIRECTORY%/%BRANCH_NAME%/quality/frameworks/webuitf
)

@mkdir %WEBUI_TMP_DIR%
@svn export %SVN_WEBUITF_REPOSITORY% %WEBUI_TMP_DIR% --force

@echo building sgtest.jar...
call ant -buildfile %SGTEST_CHECKOUT_FOLDER%\build-win.xml jar1_6 -DgsHome.dir=%BUILD_LOCATION%
@echo moving jar to lib folder in local sgtest folder
move %SGTEST_CHECKOUT_FOLDER%\output\gs-sgtest.jar %LOCAL_SGPATH%\lib