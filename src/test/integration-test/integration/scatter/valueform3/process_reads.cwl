#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool

baseCommand: process_reads.sh

inputs:
  R1_file:
    type: File
    inputBinding:
      prefix: --R1_file

  R2_file:
    type: File
    inputBinding:
      prefix: --R2_file

  output_file:
    type: string
    inputBinding:
      prefix: --OUTPUT_FILE

stdout: $(inputs.output_file)

outputs:
  upper_file:
    type: File
    outputBinding:
      glob: $(inputs.output_file)
