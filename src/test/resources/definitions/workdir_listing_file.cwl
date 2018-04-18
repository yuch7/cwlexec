class: CommandLineTool
cwlVersion: v1.0
baseCommand:
- ls
requirements:
  InitialWorkDirRequirement:
    listing:
    - class: File
      path: files/SRR1031972.bedGraph
      basename: SRR1031972.bedGraph
      
    - class: File
      path: files/SRR1031972.bedGraph.sorted
      basename: SRR1031972.bedGraph.sorted

    - class: Directory
      path: files
      basename: files
inputs: [
  ]
outputs: [
  ]