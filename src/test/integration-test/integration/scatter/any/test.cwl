class: Workflow

cwlVersion: v1.0

requirements: 
- class: ScatterFeatureRequirement
- class: InlineJavascriptRequirement
- class: StepInputExpressionRequirement

inputs:
  inFile: File 
  imageProcessingScript: string

steps:
  convertFile:
    run: file-to-array.cwl
    in:
      inputFile: inFile
    out: [fileString]
  process_images:
    run: imageProcessing.cwl
    scatter: params
    in:
      script: imageProcessingScript
      params: convertFile/fileString
    out: []

outputs: []
