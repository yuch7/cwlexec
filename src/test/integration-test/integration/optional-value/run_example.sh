#!/usr/bin/env bash

rm -rf workdir
rm -rf outdir

mkdir -p workdir
mkdir -p outdir

cwlexec -X -w $(pwd)/workdir -o $(pwd)/outdir foo_wf.cwl
exitcode=$?
if [ $exitcode -ne 0 ]; then
    echo -e "\n>>>Failed!!."
    exit 1
fi

rm -rf workdir
rm -rf outdir
