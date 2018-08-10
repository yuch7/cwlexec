#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool

requirements:
  - class: InlineJavascriptRequirement 

baseCommand: [cat]
stdout: $(inputs.other_file.nameroot)_translate.txt

inputs:
  first_file:
    type: File
    inputBinding:
      position: 1
  other_file:
    type: File
    inputBinding:
      position: 2
  
outputs:
  ofile: 
    type: File
    outputBinding:
      glob: "*_translate.txt"
