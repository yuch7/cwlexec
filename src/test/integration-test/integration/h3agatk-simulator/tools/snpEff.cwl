class: CommandLineTool
cwlVersion: v1.0

inputs:
    nodownload:
        type: boolean?
        inputBinding:
            prefix: -nodownload
            position: 5
    variant_calling_file:
        type: File
outputs:
    annotated_vcf:
        type: File
        outputBinding:
            glob: annotated_vcf.out

baseCommand: [touch, annotated_vcf.out]
