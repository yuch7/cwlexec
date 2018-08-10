#!/usr/bin/env cwltool

cwlVersion: v1.0
class: Workflow

requirements:
 - class: SubworkflowFeatureRequirement

inputs:
  inp1: File
  inp2: File

steps:
  flow1:
    run: flow1.cwl
    in:
      inp: inp1
    out: [out1file]

  flow2:
    run: flow2.cwl
    in:
      inp: inp2
    out: [out2file]

outputs:
  out1file:
    type: File
    outputSource: flow1/out1file
  out2file:
    type: File
    outputSource: flow2/out2file
