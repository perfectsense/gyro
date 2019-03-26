#!/bin/bash

mkdir -p dist
rm -rf dist/gyro-rt

cd dist

jlink --no-header-files \
    --no-man-pages \
    --add-modules java.logging,java.management,java.naming,java.scripting,java.xml,jdk.unsupported,jdk.xml.dom,java.desktop,java.instrument,java.compiler,java.sql \
    --output gyro-rt

zip -r gyro-cli-${OS_NAME}-${VERSION}.zip gyro-rt gyro
