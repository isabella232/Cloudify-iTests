@echo on


@echo deleting current apps from local sgtest folder...
if exist %LOCAL_SGPATH%\apps rmdir %LOCAL_SGPATH%\apps /s /q

@echo copying new apps to local sgtest folder...
xcopy %SGTEST_CHECKOUT_FOLDER%\apps %LOCAL_SGPATH%\apps /e /i