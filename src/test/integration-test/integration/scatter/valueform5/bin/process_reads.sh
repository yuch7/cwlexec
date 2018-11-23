#!/usr/bin/env bash

#######################
### Combining reads ###
#######################

threads=1

flags=""

while [[ ! -z $1 ]]; do
    case $1 in
		-t) threads=$2; shift;;
		-K) flags="$flags-K";;
		-Y) flags="$flags-Y";;
		-k) k=$2; flags="$flags-k$k"; shift;;
		-R) R=$2; flags="$flags-R$R"; shift;;
		*) break
    esac
    shift
done

REFFA=$1; shift
FASTQ=$1; shift
FASTQ2=$1; shift

if [[ $REFFA && $FASTQ && $FASTQ2 ]]; then
	
	echo "$flags"
	cat $FASTQ
	cat $FASTQ2

else
    >&2 echo "ERROR: Improper inputs"
    exit 1
fi
