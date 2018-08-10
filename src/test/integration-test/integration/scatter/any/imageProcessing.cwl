cwlVersion: cwl:v1.0
class: CommandLineTool
baseCommand: python
inputs:
  - id: script
    type: string
    inputBinding:
      position: 1
  - id: params
    type: Any
    inputBinding:
      position: 2
outputs: []
