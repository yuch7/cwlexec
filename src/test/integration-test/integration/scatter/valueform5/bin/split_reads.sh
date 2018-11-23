#!/usr/bin/env bash

#######################################################################
### Simulate the workflow of extracting reads from a paired-end bam ###
#######################################################################

INPUT_FILE=""
R1_FILE=""
R2_FILE=""

while [[ ! -z $1 ]]; do
    case $1 in
        --INPUT) INPUT_FILE=$2; shift;;
        --FASTQ) R1_FILE=$2; shift;;
        --SECOND_END_FASTQ) R2_FILE=$2; shift;;
    esac
    shift
done

if [[ $INPUT_FILE && $R1_FILE && $R2_FILE ]]; then

	grep 'R1' $INPUT_FILE | while read line; do
		echo "R1: $line"
	done > $R1_FILE

	grep 'R2' $INPUT_FILE | while read line; do
		echo "R2: $line"
	done > $R2_FILE

else
    >&2 echo "ERROR: Improper inputs"
    exit 1
fi
