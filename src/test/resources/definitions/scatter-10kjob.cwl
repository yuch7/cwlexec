#!/usr/bin/env cwl-runner

cwlVersion: v1.0
$graph:

- id: echo
  class: CommandLineTool
  inputs:
    echo_in1:
      type: string
      inputBinding: {}
    echo_in2:
      type: string
      inputBinding: {}
    echo_in3:
      type: string
      inputBinding: {}
    echo_in4:
      type: string
      inputBinding: {}
  outputs:
    echo_out:
      type: string
      outputBinding:
        glob: "step1_out"
        loadContents: true
        outputEval: $(self[0].contents)
  baseCommand: "echo"
  arguments: ["-n", "foo"]
  stdout: step1_out

- id: echo2
  class: CommandLineTool
  inputs:
    echo_in21:
      type: string
      inputBinding: {}
    echo_in22:
      type: string
      inputBinding: {}
    echo_in23:
      type: string
      inputBinding: {}
    echo_in24:
      type: string
      inputBinding: {}
  outputs:
    echo2_out:
      type: string
      outputBinding:
        glob: "step2_out"
        loadContents: true
        outputEval: $(self[0].contents)
  baseCommand: "echo"
  arguments: ["-n", "foo"]
  stdout: step2_out

- id: main
  class: Workflow
  inputs:
    inp1: string[]
    inp2: string[]
    inp3: string[]
    inp4: string[]
  requirements:
    - class: ScatterFeatureRequirement
  steps:
    step1:
      scatter: [echo_in1, echo_in2, echo_in3, echo_in4]
      scatterMethod: flat_crossproduct
      in:
        echo_in1: inp1
        echo_in2: inp2
        echo_in3: inp3
        echo_in4: inp4
      out: [echo_out]
      run: "#echo"
    step2:
      scatter: [echo_in21, echo_in22, echo_in23, echo_in24]
      scatterMethod: flat_crossproduct
      in:
        echo_in21: step1/echo_out
        echo_in22: inp2
        echo_in23: inp3
        echo_in24: inp4
      out: [echo2_out]
      run: "#echo2"

  outputs:
    out:
      outputSource: step2/echo2_out
      type:
        type: array
        items: string
