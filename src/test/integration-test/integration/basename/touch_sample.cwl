#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool

requirements:
 - class: InitialWorkDirRequirement
   listing:
    - entry: $(inputs.out_dir)
      writable: true

baseCommand: touch.sh

inputs:
  sample:
    type: string
    inputBinding:
      position: 1

  out_dir:
    type: Directory
    inputBinding:
      position: 2

outputs:
  foofile:
    type: File
    outputBinding:
      glob: $(inputs.out_dir.basename)/$(inputs.sample).foo

