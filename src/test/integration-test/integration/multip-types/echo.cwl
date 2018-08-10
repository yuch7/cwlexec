#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool

baseCommand: [echo]

inputs:
  foo:
    type: 
      - File
      - Directory
    inputBinding:
      position: 1

outputs: []
