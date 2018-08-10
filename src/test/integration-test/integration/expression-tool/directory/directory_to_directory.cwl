#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: ExpressionTool

requirements:
 - class: InlineJavascriptRequirement

inputs: 
  input_dir: Directory

expression: |
  ${
    return {"out_dirs": inputs.input_dir.listing};
  }
outputs:
  out_dirs: File[]
