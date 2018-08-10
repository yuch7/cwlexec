#!/usr/bin/env cwltool

cwlVersion: v1.0
class: CommandLineTool

inputs:
  inp:
    type: File
    inputBinding:
      position: 1
outputs:
  script1file:
    type: File
    outputBinding:
      glob: 1.out

baseCommand: /home/weliu/workspace/cwlexec/src/test/integration-test/integration/subflow/SubworkflowDependenciesError/script1.sh
