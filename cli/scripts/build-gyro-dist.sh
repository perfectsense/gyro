#!/bin/bash

mkdir -p dist
rm -rf dist/gyro-rt

cd dist

zip -r gyro-cli-${OS_NAME}-${VERSION}.zip gyro
