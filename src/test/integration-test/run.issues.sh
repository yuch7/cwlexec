#!/bin/sh

CWLTEST_TOP=$(pwd)
cd $CWLTEST_TOP/integration/quotes
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 0
fi
_test_count=1

cd $CWLTEST_TOP/integration/record_eval
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 1
fi
_test_count=2

cd $CWLTEST_TOP/integration/subflow/SubworkflowDependenciesError
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 2
fi
_test_count=3

cd $CWLTEST_TOP/integration/optional-value
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 3
fi
_test_count=4

cd $CWLTEST_TOP/integration/basename
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 4
fi
_test_count=5

cd $CWLTEST_TOP/integration/multip-types
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 5
fi
_test_count=6

cd $CWLTEST_TOP/integration/eval-optional-value
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 6
fi
_test_count=7

cd $CWLTEST_TOP/integration/glob_eval_patterns
./run_example.sh
exitcode=$?
if [ $exitcode -ne 0 ]; then
    exit 7
fi
_test_count=8

exit ${_test_count}
