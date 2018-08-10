#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool

baseCommand: [echo]

inputs:
  foo:
    type: string
    default: "sheep"
    inputBinding:
      position: 1

outputs:
  ofile:
    type: stdout

stdout: $(inputs.foo).txt
