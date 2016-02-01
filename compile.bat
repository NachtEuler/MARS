@echo off
javac -d classes\ src\* -Xlint:unchecked
IF %ERRORLEVEL% NEQ 0 pause