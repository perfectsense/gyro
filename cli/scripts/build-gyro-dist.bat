
mkdir dist > nul
rmdir /S dist\gyro-rt > nul

cd dist

copy ..\build\libs\gyro-cli-%VERSION%.jar gyro-cli.jar
copy ..\scripts\stub.exe gyro.exe

zip -r gyro-cli-%OS_NAME%-%VERSION%.zip gyro-cli.jar gyro.exe
