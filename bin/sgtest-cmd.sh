#!/bin/bash

BUILD_NUMBER=$1; export BUILD_NUMBER

ant  -DBUILD_DIR=${BUILD_DIR} -DBUILD_NUMBER=${BUILD_NUMBER} -f run.xml testsummary

#return java exit code.
exit $?
