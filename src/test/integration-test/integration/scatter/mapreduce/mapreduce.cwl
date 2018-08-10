#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: Workflow
requirements:
 - class: ScatterFeatureRequirement

inputs:
  upperLimit_input: int

steps:
  map:
    run: map.cwl
    in:
      upperLimit: upperLimit_input
    out: [ifiles]

  foo:
    run: foo.cwl
    scatter: ifile
    in: 
      ifile: map/ifiles
    out: [ofile]

  reduce:
    run: reduce.cwl
    in:
      ifiles: foo/ofile
    out: [final_output]

outputs:
  final_output:
    type: File
    outputSource: reduce/final_output
