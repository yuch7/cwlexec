class: CommandLineTool
cwlVersion: v1.0

inputs:
    indels_vcf:
        type: File
outputs:
    output_File:
        type: File
        outputBinding:
            glob: output_File_2.out

baseCommand: [touch, output_File_2.out]
