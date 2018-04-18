#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool
baseCommand: echo

requirements:
  - class: InlineJavascriptRequirement

inputs:
    - id: nullType
      type: 'null'
    - id: boolType
      type: boolean
      default: false
    - id: intType
      type: int
      default: 100
    - id: longType
      type: long
      default: 32000000000
    - id: floatType
      type: float
      default: 0.04
    - id: doubleType
      type: double
      default: 0.02
    - id: stringType
      type: string
      default: test
    - id: fileType
      type: File
      default:
         class: File
         path: files/data_1.fastq.bz2
    - id: dirType
      type: Directory
      default:
         class: Directory
         path: ./files
outputs: []
