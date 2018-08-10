class: CommandLineTool
cwlVersion: v1.0

inputs:
    raw_vcf:
        type: File
    recal_file:
        type: File
    tranches_file:
        type: File
outputs:
    vqsr_vcf:
        type: File
        outputBinding:
            glob: vqsr_vcf.out

baseCommand: [touch, vqsr_vcf.out]
