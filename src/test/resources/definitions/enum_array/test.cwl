cwlVersion: v1.0
class: CommandLineTool
inputs:
  - id: numbers
    type:
      type: array
      items:
        type: enum
        name: words
        symbols:
          - ONE
          - TWO
          - THREE
          - FOUR
          - FIVE
    inputBinding:
      position: 1
outputs:
  - id: example_out
    type: stdout
stdout: output.txt
baseCommand: echo