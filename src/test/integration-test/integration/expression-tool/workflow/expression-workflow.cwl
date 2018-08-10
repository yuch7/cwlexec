#!/usr/bin/env cwl-runner

cwlVersion: v1.0

class: Workflow

inputs:
- id: input_fastq
  type:
    type: array
    items: File

outputs:
- id: output_fastq
  type:
    type: array
    items: File
  outputSource: file_expression/OUTPUT

steps:
- id: file_expression
  run: expression-array.cwl
  in:
  - id: INPUT
    source: input_fastq
  out:
  - id: OUTPUT
