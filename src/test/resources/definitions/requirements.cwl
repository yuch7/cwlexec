#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool
baseCommand: echo

requirements:
  - class: InlineJavascriptRequirement
  - class: EnvVarRequirement
    envDef:
      - envName: "AL_USE_CONCATENATED_GENOME"
        envValue: $(inputs.CONCATENATED_GENOME?"1":"0")
      - envName: "AL_BWA_ALN_PARAMS"
        envValue: "-k 0 -n 0 -t 4"
      - envName: "AL_DIR_TOOLS"
        envValue: "/usr/local/bin/"
  - $import: requirements/curl-docker.yml
  - class: ShellCommandRequirement
  - class: ResourceRequirement
    ramMin: 10240
    coresMin: 8
    outdirMin: 512000
  - class: InitialWorkDirRequirement
    listing:
      - entryname: example.conf
        entry: contents
        writable: true 
hints:
  - class: InlineJavascriptRequirement
    expressionLib:
       - var new_ext = function() { var ext=inputs.bai?'.bai':inputs.csi?'.csi':'.bai';
         return inputs.input.path.split('/').slice(-1)[0].replace(/.bam$/, '')+ext; };
  - class: SoftwareRequirement
    packages:
      trimmomatic:
        specs: [ "https://identifiers.org/rrid/RRID:SCR_011848" ]
        version: [ "0.32", "0.35", "0.36" ]

inputs: []
outputs: []