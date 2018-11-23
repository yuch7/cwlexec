#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool

baseCommand: [split_reads.sh]

inputs:
  
  fastq:
    type: string
    inputBinding:
      prefix: --FASTQ

  fastq2:
    type: string
    inputBinding:
      prefix: --SECOND_END_FASTQ

  input_file:
    type: File
    inputBinding:
      prefix: --INPUT

outputs:
  fq1:
    type: File
    outputBinding:
      glob: $(inputs.fastq)

  fq2:
    type: File
    outputBinding:
      glob: $(inputs.fastq2)
