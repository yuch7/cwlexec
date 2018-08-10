class: CommandLineTool
cwlVersion: v1.0

inputs:
    haplotypecaller_snps_vcf:
        type: File
outputs:
    tranches_File:
        type: File
        outputBinding:
            glob: tranches_File.out
    recal_File:
        type: File
        outputBinding:
            glob: recal_File.out

baseCommand: [touch, tranches_File.out, recal_File.out]
