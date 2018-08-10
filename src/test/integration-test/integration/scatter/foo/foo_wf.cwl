#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: Workflow

requirements:
 - class: ScatterFeatureRequirement

inputs:
  ifiles: File[]

steps:
  foo:
    run: foo.cwl
    scatter: ifile
    in: 
      ifile: ifiles
    out: [ofile]

outputs:
  ofile:
    type: File[]
    outputSource: foo/ofile
