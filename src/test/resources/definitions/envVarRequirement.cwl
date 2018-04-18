#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool
baseCommand: echo

requirements:
  - class: InlineJavascriptRequirement
  - class: EnvVarRequirement
    envDef:
      - envName: "AL_USE_CONCATENATED_GENOME_0"
        envValue: $(inputs.CONCATENATED_GENOME?"1":"0")
      - envName: "AL_USE_CONCATENATED_GENOME_1"
        envValue: $(inputs.CONCATENATED_GENOME_1?"1":"0")
      - envName: "AL_BWA_ALN_PARAMS"
        envValue: "-k 0 -n 0 -t 4"
      - envName: "AL_DIR_TOOLS"
        envValue: "/usr/local/bin/"

inputs:
  CONCATENATED_GENOME_1:
    type: boolean
    default: true
outputs: []