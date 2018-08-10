#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool

inputs:
  ifile:
    type: File
    inputBinding:
      position: 1

outputs:
  ofile:
    type: stdout

stdout: $(inputs.ifile.nameroot)_parsed.txt

baseCommand: ["awk", "'{$5=$6=$7=$8=\"\"; print $2}'"]
