#!/usr/bin/env bash

rm -rf workdir
rm -rf outdir

mkdir -p workdir
mkdir -p outdir

cat tool1.cwl.template > tool1.cwl
echo "baseCommand: $(pwd)/script1.sh" >> tool1.cwl
cat tool2.cwl.template > tool2.cwl
echo "baseCommand: $(pwd)/script2.sh" >> tool2.cwl


cwlexec -X -w $(pwd)/workdir -o $(pwd)/outdir two_input_workflow.cwl inp.yml
exitcode=$?
if [ $exitcode -ne 0 ]; then
    echo -e "\n>>>two_input Failed!!."
    exit 1
fi

rm -rf workdir
rm -rf outdir

mkdir -p workdir
mkdir -p outdir

cwlexec -X -w $(pwd)/workdir -o $(pwd)/outdir one_input_workflow.cwl inp.yml
exitcode=$?
if [ $exitcode -ne 0 ]; then
    echo -e "\n>>>one_input Failed!!."
    exit 1
fi

rm -rf workdir
rm -rf outdir
