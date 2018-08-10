#!/usr/bin/env bash

rm -rf outdir
rm -rf workdir

mkdir -p outdir
mkdir -p workdir

cwlexec -X -w $(pwd)/workdir -o $(pwd)/outdir awk_print.cwl example.yml
exitcode=$?
if [ $exitcode -ne 0 ]; then
    echo -e "\n>>>Failed!!."
    exit 1
fi

rm -rf outdir
rm -rf workdir
