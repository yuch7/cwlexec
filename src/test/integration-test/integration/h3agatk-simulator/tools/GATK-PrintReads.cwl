class: CommandLineTool
cwlVersion: v1.0

inputs:
    bam_out:
        type: File
    bamstats_report:
        type: File
outputs:
    output_printReads:
        type: File
        outputBinding:
            glob: output_printReads.out

baseCommand: [touch, output_printReads.out]
