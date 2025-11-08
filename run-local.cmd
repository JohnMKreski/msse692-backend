@echo off
REM Run Spring Boot with local profile (PostgreSQL) after clean compile
IF "%DB_URL%"=="" echo Warning: DB_URL not set & pause
IF "%DB_USER%"=="" echo Warning: DB_USER not set & pause
IF "%DB_PASS%"=="" echo Warning: DB_PASS not set & pause
echo [local] Cleaning and compiling...
call mvnw.cmd -q clean compile
echo [local] Starting application with Maven profile...
call mvnw.cmd -Plocal spring-boot:run