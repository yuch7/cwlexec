#!/usr/bin/env bash

rm -rf outdir
rm -rf workdir

mkdir -p outdir
mkdir -p workdir

cat duplicates.cwl.template > duplicates.cwl
echo "baseCommand: $(pwd)/duplicates.sh" >> duplicates.cwl

cwlexec -w $(pwd)/workdir -o $(pwd)/outdir duplicates.cwl example.yml  
exitcode=$?
if [ $exitcode -ne 0 ]; then
    echo -e "\n>>>Failed!!."
    exit 1
fi

rm -rf outdir
rm -rf workdir
