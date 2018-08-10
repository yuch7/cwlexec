#!/usr/bin/env cwltool

cwlVersion: v1.0
class: Workflow

inputs:
  inp: File

steps:
  tool1:
    run: tool1.cwl
    in:
      inp: inp
    out: [script1file]

outputs:
  out1file:
    type: File
    outputSource: tool1/script1file
