#!/bin/bash

echo -e "\n>>>check cwltest..."
which cwltest
if [ $? -ne 0 ]; then
  echo "cwltest command not found."
  exit 1
else
  echo "cwltest is ready."
fi

echo -e "\n>>>cwlexec setup..."
cd `dirname $0`
execScriptPath=`pwd`
cd ../../../target/
packageName=`ls | grep cwlexec | grep tar.gz`
tar -xvzf $packageName
extractedFolder=`echo $packageName | sed  "s:.tar.gz::g"`
cd $extractedFolder
export CWLEXEC_HOME=`pwd`
if [ "x$CWLEXEC_HOME" == "x" ]; then
  echo "\$CWLEXEC_HOME is not defined."
  exit 1
else
  export PATH=$CWLEXEC_HOME:$PATH
  echo "cwlexec is ready."
fi

echo -e "\n>>>Clean up temp files in execution user's home dir..."
tmpFolder="~/.cwlexec"
if [ -d "$tmpFolder" ]; then
  cmd="rm -rf $tmpFolder"
  echo $cmd
  `$cmd`
  if [ $? == 0 ]; then
    echo "Temp folder is cleaned up."
  else
    echo "Temp folder clean up failed."
  fi
else
  echo "Temp folder is clean, skip cleaning up."
fi

#Environment is ready, use below code to run test

echo -e "\n>>>Go to test path..."
cmd="cd $execScriptPath"
echo $cmd
eval "$cmd"

echo -e "\n>>>Start Conformance Test..."
cwltest --test conformance_test_v1.0.yaml --tool "cwlexec" "-c execconf.json"
if [ $? -ne 0 ]; then
    echo "Fail to run conformance test."
    exit 1
fi

echo -e "\n>>>Start Integration Test..."
cwltest -n 1-15,20-21 --test integration_test.yaml --tool "cwlexec"
if [ $? -ne 0 ]; then
    echo "Fail to run integration test."
    exit 1
fi
cwltest -n 16-19,22 --test integration_test.yaml --tool "cwlexec" "-c execconf.json"
if [ $? -ne 0 ]; then
    echo "Fail to run integration test."
    exit 1
fi
cwltest -n 19 --test integration_test.yaml --tool "cwlexec" "-c integration/1st-workflow/dockerapp-execconf.json"
if [ $? -ne 0 ]; then
    echo "Fail to run docker application integration test."
    exit 1
fi

echo -e "\n>>>Run exitcode-workflow..."
echo "Test [1/1]"
cwlexec integration/exitcode-workflow.cwl#main integration/exitcode-workflow-job.yml 2>/dev/null
exitcode=$?
if [ $exitcode -ne 3 ]; then
    echo "Fail to run exitcode-workflow.cwl with $exitcode."
    exit 1
fi
rm -rf exitcode-workflow-*


#rerun flow
echo -e "\n>>>Test rerun workflow..."
echo "Test [1/1]"
export INTEGRATION_RERUN_HOME=`pwd`/integration/rerun
sed -i "s:\${INTEGRATION_RERUN_HOME}:$INTEGRATION_RERUN_HOME:g" ${INTEGRATION_RERUN_HOME}/step*.cwl
workflowid=`cwlexec ${INTEGRATION_RERUN_HOME}/rerun-wf.cwl ${INTEGRATION_RERUN_HOME}/rerun-wf-job.yml 2>&1 |sed -n 's/.*Workflow ID: \(.*\)$/\1/p'`
exitcode=`cwlexec -l $workflowid |sed -n 's/.*Exit Code: \([0-9]*\)/\1/p'`
if [ $exitcode -ne 2 ]; then
    sed -i "s:$INTEGRATION_RERUN_HOME:\${INTEGRATION_RERUN_HOME}:g" ${INTEGRATION_RERUN_HOME}/step*.cwl
    echo "First run of rerun-wf.cwl expect exit code 2, but got $exitcode."
    exit 1
fi
echo "1st try exit with 2 as expected."
echo "try to rerun the flow with id $workflowid..."
sed -i 's/"2"/"0"/g' ${INTEGRATION_RERUN_HOME}/step2.cwl
cwlexec -r $workflowid
exitcode=$?
sed -i 's/"0"/"2"/g' ${INTEGRATION_RERUN_HOME}/step2.cwl
sed -i "s:$INTEGRATION_RERUN_HOME:\${INTEGRATION_RERUN_HOME}:g" ${INTEGRATION_RERUN_HOME}/step*.cwl
rm -rf rerun-wf-*
if [ $exitcode -ne 0 ]; then
    echo "Rerun rerun-wf.cwl expect exit code 0, but got $exitcode."
    exit 1
fi
echo "2nd try exit with 0 as expected."


#ctrl-c
echo -e "\n>>>Test ctrl-c workflow..."
echo "Test [1/1]"
export INTEGRATION_CTRLC_HOME=`pwd`/integration/ctrlc
cwlexec ${INTEGRATION_CTRLC_HOME}/ctrlc-workflow.cwl#main ${INTEGRATION_CTRLC_HOME}/ctrlc-workflow-job.yml >temp.log 2>&1 &
echo "start workflow and wait 30s for step2 starting to run..."
sleep 30
step2id=`cat temp.log | sed -n 's/.*(step_2) was submitted. Job <\([0-9]*\)> is submitted.*/\1/p'`
cwlid=`ps -ef |grep cwlexec|grep java |grep ctrlc-workflow |awk '{print $2}'`
echo "cwl process id is $cwlid"
echo "step2 job id is $step2id"
echo "send signal 15 and wait 15s..."
kill -15 $cwlid
sleep 15
echo "checking job state..."
step2state=`bjobs $step2id |awk 'NR==2{print $3}'`
echo "step2 state is $step2state"
if [ "$step2state" != EXIT ];then
	echo "step2 should be EXIT when flow is killed, but got $step2state"
	exit 1
fi
echo "step2 is killed as expected."


#postfailure
echo -e "\n>>>Test postfailure  workflow..."
echo "Test [1/1]"
export INTEGRATION_POSTFAILURE_HOME=`pwd`/integration/post-failure
sed -i "s:\${INTEGRATION_POSTFAILURE_HOME}:$INTEGRATION_POSTFAILURE_HOME:g" ${INTEGRATION_POSTFAILURE_HOME}/*.json
cwlexec -c $INTEGRATION_POSTFAILURE_HOME/post_failure-conf.json $INTEGRATION_POSTFAILURE_HOME/post_failure-wf.cwl
exitcode=$?
sed -i "s:$INTEGRATION_POSTFAILURE_HOME:\${INTEGRATION_POSTFAILURE_HOME}:g" ${INTEGRATION_POSTFAILURE_HOME}/*.json
rm -rf post_failure-wf-*
if [ $exitcode -ne 0 ]; then
    echo "postfailure workflow exit with $exitcode."
    exit 1
fi


echo -e "\n>>>End of all tests."
