#!/usr/bin/env cwl-tool

cwlVersion: v1.0
class: Workflow

requirements:
  StepInputExpressionRequirement: {}
  ScatterFeatureRequirement: {}

inputs:
  input_files: File[]

steps:
  foo:
    run: foo.cwl
    scatter: [ input_file, output_filename ]
    scatterMethod: dotproduct
    in:
      input_file: input_files
      output_filename:
        source: input_files
        valueFrom: $(self.nameroot).out
    out:
      [output_file]

outputs:
  output_files:
    type: File[]
    outputSource: foo/output_file