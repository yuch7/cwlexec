#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: ExpressionTool

requirements:
 - class: InlineJavascriptRequirement

inputs: 
  input_file: File

expression: |
  ${
    return {"out_file": inputs.input_file};
  }
outputs:
  out_file: File
