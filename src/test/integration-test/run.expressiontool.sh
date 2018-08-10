#!/bin/sh

CWLTEST_TOP=$(pwd)
cd $CWLTEST_TOP/integration/expression-tool/any
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 0
fi
_test_count=1

cd $CWLTEST_TOP/integration/expression-tool/array
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 1
fi
_test_count=2

cd $CWLTEST_TOP/integration/expression-tool/directory
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 2
fi
_test_count=3

cd $CWLTEST_TOP/integration/expression-tool/file
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 3
fi
_test_count=4

cd $CWLTEST_TOP/integration/expression-tool/nested-dir
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 4
fi
_test_count=5

cd $CWLTEST_TOP/integration/expression-tool/workflow
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 5
fi
_test_count=6

cd $CWLTEST_TOP/integration/expression-tool/createdir
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 6
fi
_test_count=7

cd $CWLTEST_TOP/integration/expression-tool/createfile
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 7
fi
_test_count=8

exit ${_test_count}
