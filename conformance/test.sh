#!/bin/bash

groupadd docker
sudo gpasswd -a lsfadmin docker
source /opt/ibm/lsfsuite/lsf/conf/profile.lsf
source <(head -n265 /start_lsf_ce.sh | tail -n +7)
ROLE=master config_lsfce
ROLE=master start_lsf
lsid
lshosts

cd /cwlexec/src/test/integration-test

