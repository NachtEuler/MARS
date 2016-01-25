@echo off
javac -d classes\ src\*
IF %ERRORLEVEL% NEQ 0 pause