#!/bin/sh

rm -rf outdir
rm -rf workdir

mkdir outdir
mkdir workdir

export PATH=$(pwd):$PATH

cwlexec -w $(pwd)/workdir -o $(pwd)/outdir -pe PATH mapreduce.cwl example.json
exitcode=$?
if [ $exitcode -ne 0 ]; then
    echo -e "\n>>>Failed!!."
    exit 1
fi

rm -rf outdir
rm -rf workdir
