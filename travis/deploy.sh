#!/bin/bash -e
openssl aes-256-cbc -K $encrypted_8c1bbe484fb8_key -iv $encrypted_8c1bbe484fb8_iv \
  -in travis/codesigning.asc.enc -out travis/codesigning.asc -d

gpg --fast-import travis/codesigning.asc
mvn --settings travis/settings.xml deploy -Prelease,skipTests -DskipTests=true
