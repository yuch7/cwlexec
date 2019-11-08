#!/bin/bash

source /opt/ibm/lsfsuite/lsf/conf/profile.lsf
ls -l target
ls -l target/cwlexec-0.2.2/
cd src/test/integration-test
./run.sh
