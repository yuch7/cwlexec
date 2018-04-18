cwlVersion: v1.0
class: CommandLineTool
baseCommand: ${INTEGRATION_RERUN_HOME}/cat.sh
arguments: ["--exit-code", "2"]
inputs:
  src:
    type: File
    inputBinding:
      position: 1
outputs:
  cat_file:
    type: File
    outputBinding:
      glob: "cat.txt"
