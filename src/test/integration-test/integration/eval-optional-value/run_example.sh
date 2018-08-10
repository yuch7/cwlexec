#!/usr/bin/env bash

rm -rf outdir
rm -rf workdir

mkdir -p outdir
mkdir -p workdir

# no input given, even though it is optional and has a default, it produces an error
cwlexec -X -w $(pwd)/workdir -o $(pwd)/outdir foo_wf.cwl
exitcode=$?
if [ $exitcode -ne 0 ]; then
    echo -e "\n>>>Failed!!."
    exit 1
fi

rm -rf outdir
rm -rf workdir
