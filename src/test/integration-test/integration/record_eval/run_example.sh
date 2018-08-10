#!/usr/bin/env bash


rm -rf outdir
rm -rf workdir

mkdir outdir
mkdir workdir

cat touch_sample.cwl.template > touch_sample.cwl
echo "baseCommand: $(pwd)/touch.sh" >> touch_sample.cwl

cwlexec -X -w $(pwd)/workdir -o $(pwd)/outdir touch_sample.cwl example.yml
exitcode=$?
if [ $exitcode -ne 0 ]; then
    echo -e "\n>>>Failed!!."
    exit 1
fi

rm -rf outdir
rm -rf workdir
