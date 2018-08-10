#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool

baseCommand: [_foo.sh]

inputs:
  ifile:
    type: File
    inputBinding:
      position: 1
  foo:
    type: string
    inputBinding:
      position: 2

outputs:
  ofile:
    type: File
    outputBinding:
      glob: $(inputs.ifile.nameroot).$(inputs.foo).txt
