#!/bin/bash

function config_lsfce()
{
    # the host name from base image
    IMAGE_HOST=`awk -F'"' '/MASTER_LIST/ {print $(NF-1)}' $LSF_TOP/conf/lsf.conf`

    find $LSF_TOP/work/cluster1/logdir \
        $LSF_TOP/conf \
    -type f -print0 | xargs -0 sed -i "s/$IMAGE_HOST/$MYHOST/g"

    # make lsf read hosts file when new hosts added to cluster
    echo "LSF_HOST_CACHE_NTTL=0" >> $LSF_TOP/conf/lsf.conf
    echo "LSF_DHCP_ENV=y" >> $LSF_TOP/conf/lsf.conf
    echo "LSF_HOST_CACHE_DISABLE=y" >> $LSF_TOP/conf/lsf.conf
    echo "LSF_DYNAMIC_HOST_TIMEOUT=10m" >> $LSF_TOP/conf/lsf.conf
    echo 'LSB_RESOURCE_ENFORCE="cpu memory"' >> $LSF_TOP/conf/lsf.conf
    sed -i "s/${MYHOST}.*$/${MYHOST}   \!   \!   1   \(mg docker\)/g" $LSF_TOP/conf/lsf.cluster.cluster1
    sed -i "/End Resource/i\   docker     Boolean \(\)       \(\)          \(docker\)" $LSF_TOP/conf/lsf.shared
    # enable floating client
    sed -i "/# FLOAT_CLIENTS_ADDR_RANGE=/a\FLOAT_CLIENTS_ADDR_RANGE=*.*.*.*" $LSF_TOP/conf/lsf.cluster.cluster1
    sed -i "/# FLOAT_CLIENTS=/a\FLOAT_CLIENTS=10" $LSF_TOP/conf/lsf.cluster.cluster1
}


LSF_TOP=/opt/ibm/lsfsuite/lsf
IMAGE_HOST=`awk -F'"' '/MASTER_LIST/ {print $(NF-1)}' $LSF_TOP/conf/lsf.conf`
MYHOST=$1

config_lsfce

cp -rf $LSF_TOP /hostibm
#su lsfadmin -c "source /opt/ibm/lsf/conf/profile.lsf && cd src/test/integration-test && ./run.sh"

