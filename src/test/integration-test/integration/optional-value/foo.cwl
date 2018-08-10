#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool

baseCommand: [echo]

inputs:
  foo:
    type: string[]
    default: ["sheep", "cow"]
    inputBinding:
      position: 1

outputs:
  ofile:
    type: stdout

stdout: foo.txt
