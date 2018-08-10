#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: Workflow
requirements:
 - class: ScatterFeatureRequirement

inputs:
  foo_input: string[]?

steps:
  foo:
    run: foo.cwl
    in: 
      foo: foo_input 
    out: [ofile]

outputs:
  final_output:
    type: File
    outputSource: foo/ofile
