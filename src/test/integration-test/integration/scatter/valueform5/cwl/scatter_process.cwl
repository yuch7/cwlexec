#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: Workflow

requirements:
  - class: StepInputExpressionRequirement
  - class: InlineJavascriptRequirement
  - class: SubworkflowFeatureRequirement
  - class: ScatterFeatureRequirement

inputs:
  input_files: File[]
  reffa:
    type: File
    secondaryFiles:
      - .dict
      - .amb
      - .ann
      - .bwt
      - .pac
      - .sa
  threads:
    type: int?
    default: 1

steps:
  split_reads:
    run: split_reads.cwl
    scatter: [input_file, fastq, fastq2]
    scatterMethod: dotproduct
    in:
      input_file: input_files
      fastq:
        source: input_files
        valueFrom: $(self.nameroot)_R1.fastq.gz
      fastq2:
        source: input_files
        valueFrom: $(self.nameroot)_R2.fastq.gz
    out:
      [fq1,fq2]

  process_reads:
    run: process_reads.cwl
    scatter: [fastq, fastq2]
    scatterMethod: dotproduct
    in:
      Y: {valueFrom: $(true)}
      threads: threads
      reffa: reffa
      fastq: split_reads/fq1
      fastq2: split_reads/fq2
      output_file:
        valueFrom: |
          ${
            var s = inputs.fastq.nameroot;
            s = s.replace("_R1.fastq","");
            return s + ".out.txt";
          }
    out:
      [outfile]

outputs:
  fq1:
    type: File[]
    outputSource: split_reads/fq1
  fq2:
    type: File[]
    outputSource: split_reads/fq2
  outfile:
    type: File[]
    outputSource: process_reads/outfile
