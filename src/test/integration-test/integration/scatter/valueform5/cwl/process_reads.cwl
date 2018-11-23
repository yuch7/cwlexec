#!/usr/bin/env cwl-runner

# Modeled after https://github.com/common-workflow-language/workflows/blob/master/tools/bwa-mem.cwl
# commit d96b1bc2003d822500ef6ee7ac00a5b7ddb1ecf7

cwlVersion: v1.0
class: CommandLineTool

baseCommand: [process_reads.sh]

inputs:

  #########################
  # Required input/output #
  #########################
  reffa:
    type: File
    secondaryFiles:
        - .amb
        - .ann
        - .bwt
        - .pac
        - .sa
    inputBinding:
      position: 2

  fastq:
    type: File
    inputBinding:
      position: 3

  fastq2:
    type: File?
    inputBinding:
      position: 4

  output_file: string

  #######################
  # Optional parameters #
  #######################

  threads:
    type: int?
    inputBinding:
      position: 1
      prefix: -t
    doc: -t INT        number of threads [1]

  K:
    type: int?
    inputBinding:
      position: 1
      prefix: -K

  Y:
    type: boolean?
    inputBinding:
      position: 1
      prefix: -Y
    doc: -Y            use soft clipping for supplementary alignments

  k:
    type: int?
    inputBinding:
      position: 1
      prefix: -k
    doc: -k INT        minimum seed length [19]

  M:
    type: boolean?
    inputBinding:
      position: 1
      prefix: -M
    doc: -M            mark shorter split hits as secondary

  R:
    type: string?
    inputBinding:
      position: 1
      prefix: -R
    doc: -R STR        read group header line such as '@RG\tID:foo\tSM:bar' [null]

  I:
    type: int[]?
    inputBinding:
      position: 1
      prefix: -I
      itemSeparator: ','

stdout: $(inputs.output_file)

outputs:
  outfile:
    type: File
    outputBinding:
      glob: $(inputs.output_file)
