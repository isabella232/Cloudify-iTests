@echo on


@echo deleting current apps from local sgtest folder...
if exist %LOCAL_SGPATH%\apps\archives rmdir %LOCAL_SGPATH%\apps\archives /s /q
if exist %LOCAL_SGPATH%\apps\cloudify rmdir %LOCAL_SGPATH%\apps\cloudify /s /q
if exist %LOCAL_SGPATH%\apps\USM rmdir %LOCAL_SGPATH%\apps\USM /s /q

@echo copying new apps to local sgtest folder...
xcopy %SGTEST_CHECKOUT_FOLDER%\apps\archives %LOCAL_SGPATH%\apps\archives /e /i
xcopy %SGTEST_CHECKOUT_FOLDER%\apps\cloudify %LOCAL_SGPATH%\apps\cloudify /e /i
xcopy %SGTEST_CHECKOUT_FOLDER%\apps\USM %LOCAL_SGPATH%\apps\USM /e /i

@echo deleting current selenium jar from local sgtest folder...
del %LOCAL_SGPATH%\lib\selenium\selenium-java-*.jar

@echo copying selenium jar latest to lib in local sgtest folder...
xcopy %SGTEST_CHECKOUT_FOLDER%\lib\selenium\selenium-java-*.jar %LOCAL_SGPATH%\lib\selenium