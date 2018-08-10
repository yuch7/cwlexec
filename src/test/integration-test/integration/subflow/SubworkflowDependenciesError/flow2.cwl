#!/usr/bin/env cwltool

cwlVersion: v1.0
class: Workflow

inputs:
  inp: File

steps:
  tool2:
    run: tool2.cwl
    in:
      inp: inp
    out: [script2file]

outputs:
  out2file:
    type: File
    outputSource: tool2/script2file
