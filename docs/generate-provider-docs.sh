#!/bin/bash

PROVIDER_DOCS_DIR=provider-docs

generate() {
    echo "Generating documentation for $1"

    PROVIDER_DIR=`basename -s .git $1`

    if [ -d $PROVIDER_DIR ]; then
        rm -rf $PROVIDER_DIR
    fi

    git clone $1
    cd $PROVIDER_DIR

    touch settings.gradle
    ./gradlew referenceDocs

    cd -
}

mkdir -p $PROVIDER_DOCS_DIR && cd $PROVIDER_DOCS_DIR

for provider in `cat ../providers.txt`;
do
    generate $provider
done

cd -
