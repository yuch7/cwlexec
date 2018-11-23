#!/usr/bin/env bash

############################################################
### Simulate the workflow of processing paired-end reads ###
############################################################

R1_FILE=""
R2_FILE=""
Z_TRUE=""

while [[ ! -z $1 ]]; do
	case $1 in
		--R1_file) R1_FILE=$2; shift;;
		--R2_file) R2_FILE=$2; shift;;
		-z) Z_TRUE=Z; shift;;
	esac
	shift
done

if [[ $R1_FILE && $R2_FILE && $Z_TRUE ]]; then
	cat $R1_FILE | awk '{printf("%s.Z\n", toupper($0))}'
	cat $R2_FILE | awk '{printf("%s.Z\n", toupper($0))}'

elif [[ $R1_FILE && $R2_FILE ]]; then

	cat $R1_FILE | awk '{print toupper($0)}'
	cat $R2_FILE | awk '{print toupper($0)}'
else
	>&2 echo "ERROR: Improper inputs"
	>&2 echo "R1_FILE: $R1_FILE"
	>&2 echo "R2_FILE: $R2_FILE"
	exit 1
fi


