setlocal
set JAVA_HOME=c:\PROGRA~1\Java\jdk1.6.0_23
set LOOKUPGROUPS=sgtest-itaif-pc
set JSHOMEDIR=%~dp0gigaspaces
start cmd /c "%JSHOMEDIR%\bin\gs-agent.bat gsa.global.esm 0 gsa.gsc 0 gsa.global.gsm 0 gsa.global.lus 0 gsa.lus 2"
rem %JSHOMEDIR%\bin\gs-ui.bat
endlocal