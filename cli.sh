#!/bin/bash -e

./gradlew :daigou-service:daigou-tools:runScript -PmainClass=jiaoni.daigou.cli.DaigouCli -Dscript.args="$*"