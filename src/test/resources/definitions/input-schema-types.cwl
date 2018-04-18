#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool
baseCommand: echo

requirements:
  - class: InlineJavascriptRequirement

inputs:
  dept:
    type:
      name: JustDepts
      type: enum
      symbols: [-bg, -bga, -d]
    default: "-bg"
  strings:
    type:
      type: array
      items: string
    default:
      - "test1"
      - "test2"
  files:
    type:
      type: array
      items: File
    default:
      - class: File
        path: files/data_1.fastq.bz2
      - class: File
        path: files/data_2.fastq.bz2
outputs: []