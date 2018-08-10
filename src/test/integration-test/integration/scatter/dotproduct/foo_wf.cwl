#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: Workflow
requirements:
 - class: ScatterFeatureRequirement

inputs:
  file_input: File[]
  foo_input: string[]

steps:
  foo:
    run: foo.cwl
    scatter: [ifile, foo]
    scatterMethod: dotproduct
    in: 
      ifile: file_input
      foo: foo_input 
    out: [ofile]

outputs:
  final_output:
    type: File[]
    outputSource: foo/ofile
