#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool

baseCommand: foo

inputs:
  input_files:
    type: 
      type: array
      items: File
      inputBinding:
        prefix: --INPUT 

  output_filename:
    type: string
    inputBinding:
      prefix: --OUTPUT

outputs:
  output_file:
    type: File
    outputBinding:
      glob: $(inputs.output_filename)
