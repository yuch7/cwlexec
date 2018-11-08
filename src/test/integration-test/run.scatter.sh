#!/bin/sh

CWLTEST_TOP=$(pwd)
cd $CWLTEST_TOP/integration/scatter/cat
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 0
fi
_test_count=1

cd $CWLTEST_TOP/integration/scatter/foo
./run_example.foo.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 1
fi
_test_count=2

cd $CWLTEST_TOP/integration/scatter/mapreduce
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 2
fi
_test_count=3

cd $CWLTEST_TOP/integration/scatter/any
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 3
fi
_test_count=4

cd $CWLTEST_TOP/integration/scatter/queue
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 4
fi
_test_count=5

cd $CWLTEST_TOP/integration/scatter/flat_crossproduct
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 5
fi
_test_count=6

cd $CWLTEST_TOP/integration/scatter/dotproduct
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 6
fi
_test_count=7

cd $CWLTEST_TOP/integration/scatter/valueform0
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 1
fi
_test_count=8

cd $CWLTEST_TOP/integration/scatter/valueform1
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 1
fi
_test_count=9

cd $CWLTEST_TOP/integration/scatter/valueform2
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 1
fi
_test_count=10

cd $CWLTEST_TOP/integration/scatter/valueform3
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 1
fi
_test_count=11

exit ${_test_count}
