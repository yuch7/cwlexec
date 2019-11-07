#!/bin/bash

LSF_TOP=/opt/ibm/lsfsuite/lsf
echo 'LSB_RESOURCE_ENFORCE="cpu memory"' >> $LSF_TOP/conf/lsf.conf
IMAGE_HOST=`awk -F'"' '/MASTER_LIST/ {print $(NF-1)}' $LSF_TOP/conf/lsf.conf`
sed -i "s/${IMAGE_HOST}.*$/${IMAGE_HOST}   \!   \!   1   \(mg docker\)/g" $LSF_TOP/conf/lsf.cluster.cluster1
sed -i "/End Resource/i\   docker     Boolean \(\)       \(\)          \(docker\)" $LSF_TOP/conf/lsf.shared

source $LSF_TOP/conf/profile.lsf
source <(head -n265 /start_lsf_ce.sh | tail -n +7)
ROLE=master config_lsfce
#ROLE=master sudo start_lsf

cp -rf $LSF_TOP /hostibm
#su lsfadmin -c "source /opt/ibm/lsf/conf/profile.lsf && cd src/test/integration-test && ./run.sh"

