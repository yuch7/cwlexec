class: CommandLineTool
cwlVersion: v1.0

inputs: []
outputs:
    bamstats_report:
        type: File
        outputBinding:
            glob: bamstats_report.out

baseCommand: [touch, bamstats_report.out]
