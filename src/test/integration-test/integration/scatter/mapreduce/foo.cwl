#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool

baseCommand: [_foo.sh]

inputs:
  ifile:
    type: File
    inputBinding:
      position: 1

outputs:
  ofile:
    type: File
    outputBinding:
      glob: $(inputs.ifile.nameroot).foo.txt
