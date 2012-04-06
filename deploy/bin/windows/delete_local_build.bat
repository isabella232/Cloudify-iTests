@set VERSION=%1
@set MILESTONE=%2
@set USER_HOME=%3

@if exist %USER_HOME%\gigaspaces-cloudify-%VERSION%-%MILESTONE% rmdir %USER_HOME%\gigaspaces-cloudify-%VERSION%-%MILESTONE% /s /q