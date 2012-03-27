setlocal
set CLOUDIFYHOME=..\..\..\..\cloudify
pushd %CLOUDIFYHOME%\cloudify
call mvn clean
call mvn -DskipTests=true install
popd
copy /y %CLOUDIFYHOME%\dsl\target\dsl.jar ..\..\SGTest\tools\gigaspaces\lib\required\dsl.jar
copy /y %CLOUDIFYHOME%\usm\target\usm.jar ..\..\SGTest\tools\gigaspaces\lib\platform\usm\usm.jar
copy /y %CLOUDIFYHOME%\cli\target\cli.jar ..\..\SGTest\tools\gigaspaces\tools\cli\cli.jar
copy /y %CLOUDIFYHOME%\restful\target\rest.war ..\..\SGTest\tools\gigaspaces\tools\rest\rest.war
copy /y %CLOUDIFYHOME%\esc\target\esc.jar ..\..\SGTest\tools\gigaspaces\lib\platform\esm\esc.jar
copy /y %CLOUDIFYHOME%\esc-commands\target\esc-commands.jar ..\..\SGTest\tools\gigaspaces\tools\cli\plugins\esc\esc-commands.jar
endlocal
pause