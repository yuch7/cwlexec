class: CommandLineTool
cwlVersion: v1.0

inputs:
    raw_vcf:
        type: File
outputs:
    output_File:
        type: File
        outputBinding:
            glob: output_File.out

baseCommand: [touch, output_File.out]
