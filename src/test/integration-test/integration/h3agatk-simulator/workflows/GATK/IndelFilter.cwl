class: Workflow
cwlVersion: v1.0

requirements:
  - class: StepInputExpressionRequirement

inputs:
    snpf_genome:
        type: string
    snpf_nodownload:
        type: boolean
    haplotest_vcf:
        type: File
outputs:
    annotated_indels:
        type: File
        outputSource: snpeff_indels/annotated_vcf

steps:
    select_indels:
        run: ../../tools/GATK-SelectVariants.cwl
        in:
            raw_vcf: haplotest_vcf
        out: [output_File]
    filter_indels:
        run: ../../tools/GATK-VariantFiltration.cwl
        in:
            indels_vcf: select_indels/output_File
        out: [ output_File ]
    snpeff_indels:
        run: ../../tools/snpEff.cwl
        in:
            genome: snpf_genome
            nodownload: snpf_nodownload
            variant_calling_file: filter_indels/output_File
        out: [ annotated_vcf ]
