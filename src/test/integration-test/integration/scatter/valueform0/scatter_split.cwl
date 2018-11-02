#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: Workflow

requirements:
  - class: ScatterFeatureRequirement
  - class: StepInputExpressionRequirement
  - class: InlineJavascriptRequirement

inputs:
  input_files: File[]

steps:
  # 1) Simulate read extraction
  split_reads:
    run: split_reads.cwl
    scatter: [input_file, R1_file, R2_file]
    scatterMethod: dotproduct
    in:
      input_file: input_files
      R1_file:
        source: input_files
        valueFrom: $(self.nameroot).R1.tmp
      R2_file:
        source: input_files
        valueFrom: $(self.nameroot).R2.tmp
    out:
      [read1_file, read2_file]

  # 2) Simulate processing the reads
  process_reads:
    run: process_reads.cwl
    scatter: [R1_file, R2_file]
    scatterMethod: dotproduct
    in:
      R1_file: split_reads/read1_file
      R2_file: split_reads/read2_file
      output_file:
        valueFrom: |
          ${
            var s = inputs.R1_file.nameroot;
            s = s.replace(".R1","");
            return s + ".out";
          }
    out:
      [upper_file]

outputs:
  read1_files:
    type: File[]
    outputSource: split_reads/read1_file

  read2_files:
    type: File[]
    outputSource: split_reads/read2_file

  upper_file:
    type: File[]
    outputSource: process_reads/upper_file
