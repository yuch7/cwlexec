#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: Workflow

requirements:
 - class: ScatterFeatureRequirement

inputs:
  first_file: File
  other_files: File[]

steps:
  cat:
    run: cat.cwl
    in:
      first_file: first_file
      other_file: other_files
    scatter: other_file
    out: [ofile]

outputs:
  combined_files:
    type: File[]
    outputSource: cat/ofile
