#!/usr/bin/env bash

UpperLimit=$1
NFILES=$(( ( RANDOM % $UpperLimit ) + 1 ))

for ifile in $(seq $NFILES); do
    echo $ifile > $ifile.tmp
done
