cwlVersion: v1.0
class: CommandLineTool

baseCommand: echo
inputs:
  msg:
      type: string
      default: four
      inputBinding:
        position: 1
outputs: []
