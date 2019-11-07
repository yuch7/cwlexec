#!/bin/bash

LSF_TOP=/opt/ibm/lsf
echo 'LSB_RESOURCE_ENFORCE="cpu memory"' | sudo tee -a $LSF_TOP/conf/lsf.conf
IMAGE_HOST=`awk -F'"' '/MASTER_LIST/ {print $(NF-1)}' $LSF_TOP/conf/lsf.conf`
sed "s/${IMAGE_HOST}.*$/${IMAGE_HOST}   \!   \!   1   \(mg docker\)/g" $LSF_TOP/conf/lsf.cluster.cluster1 > /tmp/tmpfile1
cat /tmp/tmpfile1 | sudo tee $LSF_TOP/conf/lsf.cluster.cluster1
sed "/End Resource/i\   docker     Boolean \(\)       \(\)          \(docker\)" $LSF_TOP/conf/lsf.shared > /tmp/tmpfile2
cat /tmp/tmpfile2 | sudo tee $LSF_TOP/conf/lsf.shared

source $LSF_TOP/conf/profile.lsf
source <(head -n265 /opt/ibm/start_lsf_ce.sh | tail -n +7)
ROLE=master sudo config_lsfce
ROLE=master sudo start_lsf
lsid
lshosts

#su lsfadmin -c "source /opt/ibm/lsf/conf/profile.lsf && cd src/test/integration-test && ./run.sh"

