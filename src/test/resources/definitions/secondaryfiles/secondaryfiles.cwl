#!/usr/bin/env cwl-runner

cwlVersion: v1.0

requirements:
  - class: InlineJavascriptRequirement

class: CommandLineTool

inputs:
  fasta_path:
    type: File
    secondaryFiles:
      - .fai
      - .test
  fasta_test:
    type: File
    secondaryFiles:
      - ^^.fasta.fai
      - ^^.fasta.test
  input0:
    type: File
    secondaryFiles: |
      ${
        return [];
      }
  input1:
    type: File
    secondaryFiles: |
      ${
        if ((/.*\.bam$/i).test(inputs.input1.path))
          return {"path": inputs.input1.path+".bai", "class": "File"};
        return [];
      }

outputs: []

arguments:
  - valueFrom: ${
        return inputs.fasta_path.secondaryFiles[1].path + ' > ' + inputs.fasta_test.secondaryFiles[1].path;
        }
    position: 1
    shellQuote: false

baseCommand: [cat]
