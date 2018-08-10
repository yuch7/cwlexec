class: CommandLineTool
cwlVersion: v1.0

inputs: []
outputs:
    bam_out:
        type: File
        outputBinding:
            glob: bam_out.out

baseCommand: [touch, bam_out.out]
