#!/bin/bash

echo 'LSB_RESOURCE_ENFORCE="cpu memory"' >> /opt/ibm/lsf/conf/lsf.conf
IMAGE_HOST=`awk -F'"' '/MASTER_LIST/ {print $(NF-1)}' /opt/ibm/lsf/conf/lsf.conf`
sed -i "s/${IMAGE_HOST}.*$/${IMAGE_HOST}   \!   \!   1   \(mg docker\)/g" /opt/ibm/lsf/conf/lsf.cluster.cluster1
sed -i "/End Resource/i\   docker     Boolean \(\)       \(\)          \(docker\)" /opt/ibm/lsf/conf/lsf.shared

source /opt/ibm/lsf/conf/profile.lsf
source <(head -n265 /opt/ibm/lsf/start_lsf_ce.sh | tail -n +7)
ROLE=master sudo config_lsfce
ROLE=master sudo start_lsf
lsid
lshosts

#su lsfadmin -c "source /opt/ibm/lsf/conf/profile.lsf && cd src/test/integration-test && ./run.sh"

