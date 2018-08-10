class: CommandLineTool
cwlVersion: v1.0

inputs:
    inputBam_HaplotypeCaller:
        type: File
outputs:
    output_HaplotypeCaller:
        type: File
        outputBinding:
            glob: output_HaplotypeCaller.out

baseCommand: [touch, output_HaplotypeCaller.out]
