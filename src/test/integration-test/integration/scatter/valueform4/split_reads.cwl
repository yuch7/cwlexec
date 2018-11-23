#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool

baseCommand: split_reads.sh

inputs:
  R1_file:
    type: string
    inputBinding:
      prefix: --R1_file

  R2_file:
    type: string
    inputBinding:
      prefix: --R2_file

  input_file:
    type: File
    inputBinding:
      prefix: --INPUT_FILE

outputs:
  read1_file:
    type: File
    outputBinding:
      glob: $(inputs.R1_file)
  read2_file:
    type: File
    outputBinding:
      glob: $(inputs.R2_file)
