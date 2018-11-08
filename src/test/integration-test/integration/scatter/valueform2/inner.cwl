#!/usr/bin/env cwl-tool

cwlVersion: v1.0
class: Workflow

requirements:
  - class: StepInputExpressionRequirement
  - class: InlineJavascriptRequirement

inputs:
  input_files: File[]

steps:
  foo:
    run: foo.cwl
    scatter: input_file
    in:
      input_file: input_files
      output_filename:
        valueFrom: ${return inputs.input_file.nameroot + ".out";}
    out:
      [output_file]

outputs:
  output_files:
    type: File[]
    outputSource: foo/output_file
      
