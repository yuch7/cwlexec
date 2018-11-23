#!/usr/bin/env bash

#######################################################################
### Simulate the workflow of extracting reads from a paired-end bam ###
#######################################################################

INPUT_FILE=""
R1_FILE=""
R2_FILE=""

while [[ ! -z $1 ]]; do
	case $1 in
		--INPUT_FILE) INPUT_FILE=$2; shift;;
		--R1_file) R1_FILE=$2; shift;;
		--R2_file) R2_FILE=$2; shift;;
	esac
	shift
done

if [[ $INPUT_FILE && $R1_FILE && $R2_FILE ]]; then

	grep 'R1' $INPUT_FILE > $R1_FILE
	grep 'R2' $INPUT_FILE > $R2_FILE

else
	>&2 echo "ERROR: Improper inputs"
	exit 1
fi


