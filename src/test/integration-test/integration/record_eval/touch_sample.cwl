#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool

requirements:
 - class: InlineJavascriptRequirement
 - class: InitialWorkDirRequirement
   listing:
    - entry: "$({ class: 'Directory', listing: [] })"
      entryname: $(inputs.parameters.out_dir)
      writable: true

inputs:
  parameters:
    type:
      type: record
      name: params
      fields:
        sample:
          type: string
          inputBinding:
            position: 1

        out_dir:
          type: string
          inputBinding:
            position: 2
outputs:
  foo_dir:
    type: Directory
    outputBinding:
      glob: $(inputs.parameters.out_dir)

baseCommand: /home/weliu/workspace/cwlexec/src/test/integration-test/integration/record_eval/touch.sh
