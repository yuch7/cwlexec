class: Workflow
cwlVersion: v1.0

requirements:
  - class: StepInputExpressionRequirement

inputs:
    bwa_output_name:
        type: string
outputs:
    output_bam_out:
        type: File
        outputSource: bamout/bam_out
    output_bamstat:
        type: File
        outputSource: bamstat/bamstats_report
    output_printReads:
        type: File
        outputSource: PrintReads/output_printReads
    output_HaplotypeCaller:
        type: File
        outputSource: HaplotypeCaller/output_HaplotypeCaller

steps:
    bamout:
        run: ../../tools/bamout.cwl
        in:
            bam_input: bwa_output_name
        out: [bam_out]
    bamstat:
        run: ../../tools/bamstat.cwl
        in:
            bam_input: bwa_output_name
        out: [ bamstats_report ]
    PrintReads:
        run: ../../tools/GATK-PrintReads.cwl
        in:
            bam_out: bamout/bam_out
            bamstats_report: bamstat/bamstats_report
        out: [ output_printReads ]
    HaplotypeCaller:
        run: ../../tools/GATK-HaplotypeCaller.cwl
        in:
            inputBam_HaplotypeCaller: PrintReads/output_printReads
        out: [ output_HaplotypeCaller ]
