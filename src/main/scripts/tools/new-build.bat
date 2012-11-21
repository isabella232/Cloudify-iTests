@echo off

set GS_HOME=D:\gs-home

if NOT %1a==a goto _groupset  
set NEW_GROUP=sagi
goto _next1

:_groupset
set NEW_GROUP=%1

:_next1


if NOT %2a==a goto _majorbuildset
set BUILD_MAJOR_VERSION=8.0.5
goto _next2

:_majorbuildset
set BUILD_MAJOR_VERSION=%2

:_next2

if NOT %3a==a goto _milestone
set MILESTONE=m2
goto _next3

:_milestone
set MILESTONE=%3

:_next3



set BUILD_DIR="\\tarzan\builds\%BUILD_MAJOR_VERSION%"
PUSHD %BUILD_DIR%
for /f "delims=" %%x in ('dir /od /ad /b *') do set recent=%%x
POPD

@echo removing current GigaSpaces directory
@echo.
if exist %GS_HOME% (rmdir %GS_HOME% /s /q)

@echo downloading new GigaSpaces zip
@echo.
xcopy %BUILD_DIR%\%recent%\cloudify\1.5 D:\ /s /i /y > nul

set path="C:\Program Files\7-Zip\";%path%

PUSHD /d D:\
@echo extracting new GigaSpaces zip
@echo.
7z.exe x gigaspaces-cloudify-*.zip

@echo removing the zip file
@echo.
del gigaspaces-cloudify-*.zip /q > nul

@echo copying hsqldb.xml and setenv
@echo.

xcopy D:\hsqldb.xml D:\gigaspaces-cloudify-%BUILD_MAJOR_VERSION%-%MILESTONE%\config\gsa\ > nul
xcopy D:\setenv.bat D:\gigaspaces-cloudify-%BUILD_MAJOR_VERSION%-%MILESTONE%\bin\ /Y > nul

rename D:\gigaspaces-cloudify-%BUILD_MAJOR_VERSION%-%MILESTONE% gs-home

POPD
POPD

@echo done!