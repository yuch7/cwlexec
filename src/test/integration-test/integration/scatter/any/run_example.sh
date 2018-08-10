#!/bin/sh

rm -rf outdir
rm -rf workdir

mkdir -p outdir
mkdir -p workdir

cat test.yaml.template > test.yaml
echo "imageProcessingScript: $(pwd)/MXtasksModLSF.py" >> test.yaml

cwlexec -w $(pwd)/workdir -o $(pwd)/outdir test.cwl test.yaml 
exitcode=$?
if [ $exitcode -ne 0 ]; then
    echo -e "\n>>>Failed!!."
    exit 1
fi

rm -rf outdir
rm -rf workdir
