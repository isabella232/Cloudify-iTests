@echo off
rem taskkill /fi "STATUS eq RUNNING" /f /im javaw.exe
pushd "%~dp0"
setlocal
echo checking java version
if not exist %temp%\javaver.txt goto checkjavaver
del %temp%\javaver.txt
:checkjavaver
"%JAVA_HOME%/bin/java" -version 2>%temp%\javaver.txt
findstr 1.6 %temp%\javaver.txt
if ERRORLEVEL 1 goto javavererr
goto javaok
:javavererr
echo wrong java version. use java 1.6
echo using: 
java -version
goto exit
:javaok


rem check gigaspaces\tools\build.properties build number
pushd ..\..\gigaspaces\tools
if not exist build.properties goto gs_build
FOR /F "eol=; tokens=2,2 delims==" %%i IN ('findstr /i "buildnumber" build.properties') DO set buildnumber1=%%i
FOR /F "eol=; tokens=1,2 delims=-" %%i IN ('findstr /i "version" build.properties') DO set versionnumber1=%%i
FOR /F "eol=; tokens=2,2 delims= " %%i IN ('echo %versionnumber1%') DO set versionnumber1=%%i
rem check if gigaspaces\src corresponds to build.properties. 
findstr /i /c:%buildnumber1% ..\src\java\com\j_spaces\kernel\PlatformVersion.java
rem If not found then rebuild
if ERRORLEVEL 1 goto gs_build
findstr /i /c:%versionnumber1% ..\src\java\com\j_spaces\kernel\PlatformVersion.java
rem If not found then rebuild
if ERRORLEVEL 1 goto gs_build
goto no_gs_build
:gs_build
echo building gigaspaces
call ant
FOR /F "eol=; tokens=2,2 delims==" %%i IN ('findstr /i "buildnumber" build.properties') DO set buildnumber1=%%i
if not exist ..\releases\build_%buildnumber1%\jars\1.5\xap\gs-runtime.jar goto compile_error
if not exist ..\releases\build_%buildnumber1%\jars\1.5\xap\gs-boot.jar goto compile_error
:no_gs_build
FOR /F "eol=; tokens=2,2 delims==" %%i IN ('findstr /i "buildnumber" build.properties') DO set buildnumber=%%i
FOR /F "eol=; tokens=2,2 delims==" %%i IN ('findstr /i "milestone" build.properties') DO set milestone=%%i
FOR /F "eol=; tokens=1,2 delims=-" %%i IN ('findstr /i "version" build.properties') DO set versionnumber=%%i
FOR /F "eol=; tokens=2,2 delims= " %%i IN ('echo %versionnumber%') DO set versionnumber=%%i
echo versionnumber=%versionnumber% milestone=%milestone% buildnumber=%buildnumber%
popd

rem check we have a copy of xap from tarzan
if not exist gigaspaces goto copy

rem check we have the correct xap version from tarzan
pushd gigaspaces
cd bin
call setenv.bat
if not exist %temp%\pv.txt goto cont1
del /q %temp%\pv.txt
:cont1
call %JAVACMD% -classpath %GS_JARS% com.j_spaces.kernel.PlatformVersion > %temp%\pv.txt
FOR /F "eol=) tokens=7" %%i IN ('findstr /i "build" %temp%\pv.txt') DO set gs_buildnumber=%%i
set gs_buildnumber=%gs_buildnumber:~0,-1%
popd
if %buildnumber%==%gs_buildnumber% goto nocopy
rem delete old xap version
rmdir /s /q gigaspaces
if exist gigaspaces goto jar_locked_error
:copy
echo copying new version of xap from tarzan
if exist gigaspaces-*%buildnumber%.zip goto unzip
copy "\\tarzan\builds\%versionnumber%\build_%buildnumber%\cloudify\1.5\gigaspaces-cloudify-*%buildnumber%.zip"
if exist gigaspaces-cloudify-*%buildnumber%.zip goto unzip
goto exit
:unzip
unzip\unzip gigaspaces-*%buildnumber%.zip
del gigaspaces-*%buildnumber%.zip
move gigaspaces*%milestone% gigaspaces
:nocopy

echo compiling openspaces
pushd ..\..\openspaces
rem if not "%1"=="clean" goto del_jar
call ant clean
rem :del_jar
if not exist lib\required\gs-openspaces.jar goto del_src
del /q lib\required\gs-openspaces.jar
:del_src
if not exist lib\optional\openspaces\gs-openspaces-src.zip goto build_openspaces
del /q lib\optional\openspaces\gs-openspaces-src.zip
:build_openspaces
call ant buildmain,fulljar,srczip
if not exist lib\required\gs-openspaces.jar goto compile_error
if not exist lib\optional\openspaces\gs-openspaces-src.zip goto compile_error
echo copying lib\required\gs-runtime.jar
del /f ..\SGTest\tools\gigaspaces\lib\required\gs-runtime.jar
if exist ..\SGTest\tools\gigaspaces\lib\required\gs-runtime.jar goto jar_locked_error
copy /y ..\gigaspaces\releases\build_%buildnumber%\jars\1.5\xap\gs-runtime.jar ..\SGTest\tools\gigaspaces\lib\required\*.jar
echo copying lib\platform\boot\gs-boot.jar
del /f ..\SGTest\tools\gigaspaces\lib\platform\boot\gs-boot.jar
if exist ..\SGTest\tools\gigaspaces\lib\platform\boot\gs-boot.jar goto jar_locked_error
copy /y ..\gigaspaces\releases\build_%buildnumber%\jars\1.5\xap\gs-boot.jar ..\SGTest\tools\gigaspaces\lib\platform\boot\*.jar
echo copying lib\required\gs-openspaces.jar
del /f ..\SGTest\tools\gigaspaces\lib\required\gs-openspaces.jar
if exist ..\SGTest\tools\gigaspaces\lib\required\gs-openspaces.jar goto jar_locked_error
copy /y lib\required\gs-openspaces.jar ..\SGTest\tools\gigaspaces\lib\required\*.jar
echo copying lib\optional\openspaces\gs-openspaces-src.zip
del /f ..\SGTest\tools\gigaspaces\lib\optional\openspaces\gs-openspaces-src.zip
if exist ..\SGTest\tools\gigaspaces\lib\optional\openspaces\gs-openspaces-src.zip goto jar_locked_error
copy /y lib\optional\openspaces\gs-openspaces-src.zip ..\SGTest\tools\gigaspaces\lib\optional\openspaces\*.zip

goto end_openspace_compile
:compile_error
echo compilation error
goto exit
:jar_locked_error
echo jar locked error
goto exit
:end_openspace_compile
popd
echo generated by tests and needs to be regenerated after build next time the test runs
if not exist gigaspaces-xap-premium*%buildnumber%.zip goto refresh_eclipse_notice
del gigaspaces-xap-premium*%buildnumber%.zip
:refresh_eclipse_notice
echo DONT FORGET TO REFRESH THE ECLIPSE SGTEST PROJECT
echo updating maven
setlocal
set M2_HOME=%~dp0gigaspaces\tools\maven\apache-maven-3.0.2
set PATH=%M2_HOME%\bin;%PATH%
pushd gigaspaces\tools\maven
call installmavenrep.bat
popd
endlocal
:exit
endlocal
popd

pause