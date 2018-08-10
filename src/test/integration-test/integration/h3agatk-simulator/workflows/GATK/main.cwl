class: Workflow
cwlVersion: v1.0

requirements:
  - class: StepInputExpressionRequirement
  - class: InlineJavascriptRequirement
  - class: SubworkflowFeatureRequirement

inputs:
    bwa_output_name:
        type: string
        default: "bwa_output_name"
    snpf_nodownload:
        type: boolean
    snpf_genome:
        type: string
        default: "snpf_genome"

outputs:
    output_bamstat:
        type: File
        outputSource: HaplotypeCaller/output_bamstat
    output_printReads:
        type: File
        outputSource: HaplotypeCaller/output_printReads
    output_HaplotypeCaller:
        type: File
        outputSource: HaplotypeCaller/output_HaplotypeCaller
    output_SnpVQSR_recal_File:
        type: File
        outputSource: SnpVQSR/recal_File
    output_SnpVQSR_annotated_snps:
        type: File
        outputSource: SnpVQSR/annotated_snps
    output_IndelFilter_annotated_indels:
        type: File
        outputSource: IndelFilter/annotated_indels

steps:
    HaplotypeCaller:
        run: HaplotypeCaller.cwl
        in:
            bwa_output_name: bwa_output_name
        out: [output_bamstat, output_printReads, output_HaplotypeCaller]
    
    SnpVQSR:
        run: SnpVQSR.cwl
        in:
            snpf_genome: snpf_genome
            haplotest_vcf: HaplotypeCaller/output_HaplotypeCaller
        out: [recal_File, annotated_snps]
    
    IndelFilter:
        run: IndelFilter.cwl
        in:
            snpf_genome: snpf_genome
            snpf_nodownload: snpf_nodownload
            haplotest_vcf: HaplotypeCaller/output_HaplotypeCaller
        out: [ annotated_indels ]
