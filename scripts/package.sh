#!/bin/sh

mkdir -p ../dist
cat stub.sh ../target/beam-*-capsule-full.jar > ../dist/beam
chmod +x ../dist/beam
