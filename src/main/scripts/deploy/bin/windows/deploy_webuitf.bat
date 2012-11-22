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

@echo deploying webuitf...
pushd %WEBUI_TMP_DIR%
mvn clean install s3client:deploy -U
popd