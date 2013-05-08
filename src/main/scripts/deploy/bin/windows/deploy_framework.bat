@echo on

set FRAMEWORK_TMP_DIR=%BUILD_DIR%

@echo cloning framework
set GIT_SSL_NO_VERIFY=true
pushd %FRAMEWORK_TMP_DIR%

if %BRANCH_NAME%==trunk (
    call C:\Git\bin\git.exe clone --depth 1 https://github.com/CloudifySource/iTests-Framework.git
) else (
    call C:\Git\bin\git.exe clone -b %BRANCH_NAME% --depth 1 https://github.com/CloudifySource/iTests-Framework.git
)

popd
set iTests_Framework=%FRAMEWORK_TMP_DIR%\iTests-Framework

@echo deploying framework...
pushd %iTests_Framework%
mvn clean install s3client:deploy -U -Dbuild.home=%BUILD_DIR%
popd

rmdir /s /q %iTests_Framework%

:_skip