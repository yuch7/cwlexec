#!/usr/bin/env bash

rm -rf outdir
rm -rf workdir

mkdir -p outdir/MySample
mkdir -p workdir

export PATH=$(pwd):$PATH
cwlexec -X -w $(pwd)/workdir -o $(pwd)/outdir -pe PATH touch_sample.cwl example.yml
exitcode=$?
if [ $exitcode -ne 0 ]; then
    echo -e "\n>>>one_input Failed!!."
    exit 1
fi

rm -rf outdir
rm -rf workdir
