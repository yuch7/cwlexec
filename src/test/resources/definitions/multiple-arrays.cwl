#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: ExpressionTool

requirements:
  - class: InlineJavascriptRequirement

inputs:
  number:
    type: int
    label: a positive integer
    default: 4

outputs:
  int_array:
    type: int[]
  str_array:
    type: string[]

expression: |
  ${
    var s_arr = [], i_arr = [];
    for (var i = 0; i < inputs.number; i++) {
      s_arr.push('hello' + i + '.txt');
      i_arr.push(i);
    }
    return { "int_array": i_arr, "str_array": s_arr };
  }