@echo on


@echo deleting current apps from local sgtest folder...
if exist %LOCAL_SGPATH%\apps\archives rmdir %LOCAL_SGPATH%\apps\archives /s /q

@echo copying new apps to local sgtest folder...
xcopy %SGTEST_CHECKOUT_FOLDER%\apps\archives %LOCAL_SGPATH%\apps\archives /e /i

@echo deleting current selenium jar from local sgtest folder...
del %LOCAL_SGPATH%\lib\selenium\selenium-java-*.jar

@echo copying selenium jar latest to lib in local sgtest folder...
xcopy %SGTEST_CHECKOUT_FOLDER%\lib\selenium\selenium-java-*.jar %LOCAL_SGPATH%\lib\selenium