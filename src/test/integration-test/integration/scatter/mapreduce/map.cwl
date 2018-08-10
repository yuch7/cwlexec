#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool

baseCommand: [_mapper.sh]

inputs:
  upperLimit:
    type: int
    inputBinding:
      position: 1

outputs:
  ifiles:
    type: File[]
    outputBinding:
      glob: "*.tmp"
