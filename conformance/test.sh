#!/bin/bash

ls /root
exit 0

groupadd docker
gpasswd -a lsfadmin docker
source /opt/ibm/lsfsuite/lsf/conf/profile.lsf
source <(head -n265 /start_lsf_ce.sh | tail -n +7)
ROLE=master config_lsfce
ROLE=master start_lsf
lsid
lshosts

cd /home/lsfadmin/cwlexec/src/test/integration-test
su lsfadmin -c "source /opt/ibm/lsfsuite/lsf/conf/profile.lsf && ./run.sh"

