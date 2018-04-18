#!/usr/bin/env cwl-runner

cwlVersion: v1.0

requirements:
  - class: InlineJavascriptRequirement
  - class: ShellCommandRequirement

class: CommandLineTool

inputs: []
outputs: []
arguments:
  - valueFrom: "find . -name \"*.cwl\""
    position: 1
    shellQuote: false
  - valueFrom: "|"
    position: 2
    shellQuote: false
  - valueFrom: "xargs grep \"ShellCommandRequirement\"" 
    position: 3
    shellQuote: false

baseCommand: []
