@echo off
REM Run Spring Boot with prod profile (PostgreSQL) after clean compile
IF "%DB_URL%"=="" echo Warning: DB_URL not set & pause
IF "%DB_USER%"=="" echo Warning: DB_USER not set & pause
IF "%DB_PASS%"=="" echo Warning: DB_PASS not set & pause
echo [prod] Cleaning and compiling...
call mvnw.cmd -q clean compile
echo [prod] Starting application with Maven profile...
call mvnw.cmd -Pprod spring-boot:run