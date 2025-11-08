@echo off
REM Generic runner: run-profile.cmd <dev|local|prod>
IF "%1"=="" (
  echo Usage: run-profile.cmd ^<dev^|local^|prod^>
  exit /b 1
)
set PROFILE=%1
echo [profile=%PROFILE%] Cleaning and compiling...
call mvnw.cmd -q clean compile
echo [profile=%PROFILE%] Starting application with Maven profile...
call mvnw.cmd -P%PROFILE% spring-boot:run