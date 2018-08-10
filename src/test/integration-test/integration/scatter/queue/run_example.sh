#!/usr/bin/env bash

rm -rf outdir
rm -rf workdir

mkdir -p outdir
mkdir -p workdir

export PATH=$(pwd):$PATH
export LSB_DEFAULTQUEUE=short

cwlexec -w $(pwd)/workdir -o $(pwd)/outdir -c LSF.json -pe PATH foo_wf.cwl foo_example.yml
exitcode=$?
if [ $exitcode -ne 0 ]; then
    echo -e "\n>>>Failed!!."
    exit 1
fi

rm -rf outdir
rm -rf workdir
