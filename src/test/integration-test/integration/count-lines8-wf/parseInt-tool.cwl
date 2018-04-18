#!/usr/bin/env cwl-runner

class: CommandLineTool
requirements:
  - class: InlineJavascriptRequirement
cwlVersion: v1.0

inputs:
  file1:
    type: File
    inputBinding: { loadContents: true }

outputs:
  - id: output
    type: Any
    outputBinding:
      outputEval: $(parseInt(inputs.file1.contents)) 

baseCommand: "true"
