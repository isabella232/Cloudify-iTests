@echo on

@rem - Replace default configuration files with local-machine specific files.

set WEBUI_TMP_DIR=%SGTEST_CHECKOUT_FOLDER%\apps

@echo cloning webuitf

@mkdir %WEBUI_TMP_DIR%
set GIT_SSL_NO_VERIFY=true
pushd %WEBUI_TMP_DIR%
call C:\Git\bin\git.exe clone --depth 1 https://github.com/CloudifySource/Cloudify-iTests-webuitf.git
popd
set Cloudify_iTests_webuitf=%WEBUI_TMP_DIR%/Cloudify-iTests-webuitf

@echo deploying webuitf...
pushd %WEBUI_TMP_DIR%
mvn clean install s3client:deploy -U
rmdir /s /q %WEBUI_TMP_DIR%
popd


:_skip