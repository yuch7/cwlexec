#!/usr/bin/env bash

rm -rf outdir
rm -rf workdir

mkdir -p outdir
mkdir -p workdir

export PATH=$(pwd):$PATH

cwlexec -w $(pwd)/workdir -o $(pwd)/outdir -pe PATH scatter_split.cwl ex.02.yml.tmp
exitcode=$?
if [ $exitcode -ne 0 ]; then
    echo -e "\n>>>Failed!!."
    exit 1
fi

rm -rf outdir
rm -rf workdir
