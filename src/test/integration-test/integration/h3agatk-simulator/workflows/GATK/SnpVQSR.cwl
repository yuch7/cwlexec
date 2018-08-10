class: Workflow
cwlVersion: v1.0

requirements:
  - class: StepInputExpressionRequirement

inputs:
    snpf_genome:
        type: string
    haplotest_vcf:
        type: File
outputs:
    recal_File:
        type: File
        outputSource: vqsr_snps/recal_File
    annotated_snps:
        type: File
        outputSource: snpeff_snps/annotated_vcf

steps:
    vqsr_snps:
        run: ../../tools/GATK-VariantRecalibrator-SNPs.cwl
        in:
            snpf_genome: snpf_genome
            haplotypecaller_snps_vcf: haplotest_vcf
        out: [tranches_File, recal_File]
    apply_recalibration_snps:
        run: ../../tools/GATK-ApplyRecalibration.cwl
        in:
            snpf_genome: snpf_genome
            raw_vcf: haplotest_vcf
            recal_file: vqsr_snps/recal_File
            tranches_file: vqsr_snps/tranches_File
        out: [ vqsr_vcf ]
    snpeff_snps:
        run: ../../tools/snpEff.cwl
        in:
            genome: snpf_genome
            variant_calling_file: apply_recalibration_snps/vqsr_vcf
        out: [ annotated_vcf ]
