#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool
baseCommand: echo

requirements:
- class: InlineJavascriptRequirement

inputs:
  key:
    type:
      type: array
      items:
        type: array
        items: string
        inputBinding:
          prefix: -kk
      inputBinding:
        prefix: -k
    inputBinding:
      position: 1
    default:
      - ["key1", "key2"]
      - ["key3", "key4"]
outputs: []