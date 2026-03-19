@echo off
setlocal

echo ==========================================
echo 开始执行 Maven clean install
echo ==========================================
call mvn -s .mvn/settings.xml clean install -DskipTests

if errorlevel 1 (
    echo.
    echo clean install 执行失败，脚本终止。
    exit /b %errorlevel%
)

echo.
echo ==========================================
echo clean install 执行完成
echo 开始启动 spring-boot-iot-admin
echo ==========================================
call mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run "-Dspring-boot.run.profiles=dev"

if errorlevel 1 (
    echo.
    echo spring-boot 启动失败。
    exit /b %errorlevel%
)

endlocal