#!/bin/bash

rm -f test.*.log

LOG_FILE="test.`date "+%Y-%m-%d-%H-%M"`.log"
CODE_REPO=`dirname $(dirname $(dirname $(pwd)))`
TEST_TOP=$(pwd)

function log() {
    echo -e "[`date "+%Y-%m-%d %H:%M:%S"`]  $*" | tee -a $LOG_FILE
}

log "check cwltest ..."
which cwltest >/dev/null 2>&1
if [ $? -ne 0 ]; then
  log "cwltest is required."
  exit 1
else
  log "cwltest is ready."
fi

log "prepare cwlexec ..."
which cwlexec >/dev/null 2>&1
if [ $? -ne 0 ]; then
  log "try to prepare cwlexec from $CODE_REPO/target."
  if [ -d $CODE_REPO/target ]; then
    cd $CODE_REPO/target
    cwlexec_pkg=`ls | grep cwlexec | grep tar.gz`
    tar -zxf $cwlexec_pkg
    CWLEXEC_HOME=$CODE_REPO/target/`echo $cwlexec_pkg | sed  "s:.tar.gz::g"`
    export PATH=$CWLEXEC_HOME:$PATH
    cd $TEST_TOP
  else
    log "cwlexec is not found, build it firstly."
    exit 1
  fi
fi

#check the cwlexec again, if it still cannot be found, exit
which cwlexec >/dev/null 2>&1
if [ $? -ne 0 ]; then
    log "cwlexec is required."
fi
log "cwlexec (`which cwlexec`) is ready."

if [ "$1"x = "--with-cleanup"x ]; then
    log "cleanup cwlexec workdir and database dir ..."
    rm -rf ~/.cwlexec
    rm -rf ~/cwl-workdir
    log "cleanup is done."
fi

log "Start Conformance Test ..."
cwltest --test conformance_test_v1.0.yaml --tool "cwlexec" "-c execconf.json"
if [ $? -ne 0 ]; then
    log "Failed to run conformance test."
    exit 1
fi

log "Start Integration Test ..."
cwltest -n 1-15,20-21 --test integration_test.yaml --tool "cwlexec"
if [ $? -ne 0 ]; then
    log "Failed to run integration test [1-15,20-21]."
    exit 1
fi
cwltest -n 16-19,22 --test integration_test.yaml --tool "cwlexec" "-c execconf.json"
if [ $? -ne 0 ]; then
    log "Failed to run integration test [16-19,22]."
    exit 1
fi
#cwltest -n 19 --test integration_test.yaml --tool "cwlexec" "-c integration/1st-workflow/dockerapp-execconf.json"
#if [ $? -ne 0 ]; then
#    log "Failed to run docker application integration test."
#    exit 1
#fi

log "Start Expression Tool Test ..."
./run.expressiontool.sh >/dev/null 2>&1
_test_count=$?
if [ $_test_count -ne 8 ]; then
    log "Failed to run expression tool test [$(($_test_count + 1))]"
    exit 1
fi
echo -e "All tests [${_test_count}/8] passed."

log "Start Scatter Test ..."
./run.scatter.sh >/dev/null 2>&1
_test_count=$?
if [ $_test_count -ne 11 ]; then
    log "Failed to run scatter test [$(($_test_count + 1))]"
    exit 1
fi
echo -e "All tests [${_test_count}/11] passed"

log "Start Issue Test ..."
./run.issues.sh >/dev/null 2>&1
_test_count=$?
if [ $_test_count -ne 8 ]; then
    log "Failed to run scatter test [$(($_test_count + 1))]"
    exit 1
fi
echo -e "All tests [${_test_count}/8] passed."

log "Start required_args Test ..."
cwlexec integration/required_args.cwl 2>/dev/null
exitcode=$?
if [ $exitcode -ne 252 ]; then
    log "Failed to run required_args.cwl with $exitcode."
    exit 1
fi

log "Start h3agatk-simulator Test ..."
rm -rf h3agatk-simulator-output
mkdir -p h3agatk-simulator-output
cwlexec -o $(pwd)/h3agatk-simulator-output integration/h3agatk-simulator/workflows/GATK/main.cwl integration/h3agatk-simulator/workflows/GATK/main.json >/dev/null 2>&1
exitcode=$?
if [ $exitcode -ne 0 ]; then
    log "Failed to run h3agatk-simulator with $exitcode."
    exit 1
fi
rm -rf h3agatk-simulator-output

log "Start Exit Code Test ..."
cwlexec integration/exitcode-workflow.cwl#main integration/exitcode-workflow-job.yml 2>/dev/null
exitcode=$?
if [ $exitcode -ne 3 ]; then
    log "Failed to run exit code test with $exitcode."
    exit 1
fi
rm -rf exitcode-workflow-*

# rerun flow
# 1st try exit with 2 as expected.
# 2nd try exit with 0 as expected.
log "Start Rerun Test ..."
INTEGRATION_RERUN_HOME=`pwd`/integration/rerun
sed -i "s:\${INTEGRATION_RERUN_HOME}:$INTEGRATION_RERUN_HOME:g" ${INTEGRATION_RERUN_HOME}/step*.cwl
workflowid=`cwlexec ${INTEGRATION_RERUN_HOME}/rerun-wf.cwl ${INTEGRATION_RERUN_HOME}/rerun-wf-job.yml 2>&1 |sed -n 's/.*Workflow ID: \(.*\)$/\1/p'`
exitcode=`cwlexec -l $workflowid |sed -n 's/.*Exit Code: \([0-9]*\)/\1/p'`
if [ $exitcode -ne 2 ]; then
    sed -i "s:$INTEGRATION_RERUN_HOME:\${INTEGRATION_RERUN_HOME}:g" ${INTEGRATION_RERUN_HOME}/step*.cwl
    log "Failed to run rerun test, first run of rerun-wf.cwl expect exit code 2, but got $exitcode."
    exit 1
fi
sed -i 's/"2"/"0"/g' ${INTEGRATION_RERUN_HOME}/step2.cwl
cwlexec -r $workflowid
exitcode=$?
sed -i 's/"0"/"2"/g' ${INTEGRATION_RERUN_HOME}/step2.cwl
sed -i "s:$INTEGRATION_RERUN_HOME:\${INTEGRATION_RERUN_HOME}:g" ${INTEGRATION_RERUN_HOME}/step*.cwl
rm -rf rerun-wf-*
if [ $exitcode -ne 0 ]; then
    log "Failed to run rerun test, rerun rerun-wf.cwl expect exit code 0, but got $exitcode."
    exit 1
fi

log "Start Post Failure Test ..."
INTEGRATION_POSTFAILURE_HOME=`pwd`/integration/post-failure
sed -i "s:\${INTEGRATION_POSTFAILURE_HOME}:$INTEGRATION_POSTFAILURE_HOME:g" ${INTEGRATION_POSTFAILURE_HOME}/*.json
cwlexec -c $INTEGRATION_POSTFAILURE_HOME/post_failure-conf.json $INTEGRATION_POSTFAILURE_HOME/post_failure-wf.cwl
exitcode=$?
sed -i "s:$INTEGRATION_POSTFAILURE_HOME:\${INTEGRATION_POSTFAILURE_HOME}:g" ${INTEGRATION_POSTFAILURE_HOME}/*.json
rm -rf post_failure-wf-*
if [ $exitcode -ne 0 ]; then
    log "Failed to run post failure test, postfailure workflow exit with $exitcode."
    exit 1
fi

log "All of tests are passed."
