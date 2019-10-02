
mkdir dist > nul
rmdir /S dist\gyro-rt > nul

cd dist

copy ..\build\libs\gyro-cli-%VERSION%.jar gyro-cli.jar
copy ..\scripts\stub.exe gyro.exe

jlink --no-header-files^
    --no-man-pages^
    --add-modules java.logging,java.management,java.naming,java.scripting,java.xml,jdk.unsupported,jdk.xml.dom,java.desktop,java.instrument,java.compiler,java.sql,jdk.crypto.ec^
    --output gyro-rt

zip -r gyro-cli-%OS_NAME%-%VERSION%.zip gyro-rt gyro-cli.jar gyro.exe
