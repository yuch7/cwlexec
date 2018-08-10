#!/bin/sh

rm -rf outdir
rm -rf workdir

mkdir -p outdir
mkdir -p workdir

cwlexec -w $(pwd)/workdir -o $(pwd)/outdir dir7.cwl dir7.yml 
exitcode=$?
if [ $exitcode -ne 0 ]; then
    echo -e "\n>>>Failed!!."
    exit 1
fi

rm -rf outdir
rm -rf workdir
