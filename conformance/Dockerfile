#FROM ibmcom/lsfce:10.2.0
#USER root

FROM ubuntu

RUN echo $CWLTOP
RUN echo ${CWLTOP}
ADD . /root/cwlexec

#RUN apt update
#RUN apt install -y apt-transport-https ca-certificates curl gnupg-agent software-properties-common
#RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -
#RUN add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
#RUN apt update && apt install -y docker-ce docker-ce-cli containerd.io
#RUN apt install -y python3 python3-pip
#RUN pip3 install cwltest

#ADD --chown=lsfadmin:lsfadmin ${CWLTOP} /home/lsfadmin/cwlexec
ADD test.sh /
ENTRYPOINT ["/test.sh"]

