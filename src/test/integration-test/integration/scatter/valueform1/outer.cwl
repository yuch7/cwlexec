#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: Workflow

requirements:
  - class: SubworkflowFeatureRequirement
  - class: ScatterFeatureRequirement

inputs:
  files: File[]

steps:
  scatter_flow:
    run: inner.cwl
    in:
      input_files: files
    out:
      [output_files]

outputs:
  output_files:
    type: File[]
    outputSource: scatter_flow/output_files

