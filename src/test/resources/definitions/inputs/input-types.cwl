#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool
baseCommand: echo

requirements:
  - class: InlineJavascriptRequirement

inputs:
  - id: var0
    type: string?
  - id: var1
    type: string?
  - id: var2
    type: string[]
  - id: var3
    type: string[]?
  - id: var4
    type: string[]?
  - id: var5
    type:
      - 'null'
      - string
      - type: array
        items: string
outputs: []
