cwlVersion: v1.0
class: CommandLineTool
baseCommand: ${INTEGRATION_RERUN_HOME}/append.sh
inputs:
  src:
    type: File
    inputBinding:
      position: 1
outputs:
  append_file:
    type: File
    outputBinding:
      glob: "*.out"
