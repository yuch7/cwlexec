#!/usr/bin/env cwltool

cwlVersion: v1.0
class: CommandLineTool

inputs:
  inp:
    type: File
    inputBinding:
      position: 1
outputs:
  script2file:
    type: File
    outputBinding:
      glob: 2.out

baseCommand: /home/weliu/workspace/cwlexec/src/test/integration-test/integration/subflow/SubworkflowDependenciesError/script2.sh
