#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool

inputs:
  ifiles:
    type: File[]
    inputBinding:
      position: 1

baseCommand: [_reducer.sh]

outputs:
  final_output:
    type: File
    outputBinding:
      glob: out.out
