pushd ..\..\cloudify\cloudify
call mvn clean
call mvn -DskipTests=true install
popd
copy /y ..\..\cloudify\dsl\target\dsl.jar ..\..\SGTest\tools\gigaspaces\lib\required\dsl.jar
copy /y ..\..\cloudify\usm\target\usm.jar ..\..\SGTest\tools\gigaspaces\lib\platform\usm\usm.jar
copy /y ..\..\cloudify\cli\target\cli.jar ..\..\SGTest\tools\gigaspaces\tools\cli\cli.jar
copy /y ..\..\cloudify\restful\target\rest.war ..\..\SGTest\tools\gigaspaces\tools\rest\rest.war
copy /y ..\..\cloudify\esc\target\esc.jar ..\..\SGTest\tools\gigaspaces\lib\platform\esm\esc.jar
copy /y ..\..\cloudify\esc-commands\target\esc-commands.jar ..\..\SGTest\tools\gigaspaces\tools\cli\plugins\esc\esc-commands.jar
pause