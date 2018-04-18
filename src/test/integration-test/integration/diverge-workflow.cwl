cwlVersion: v1.0
$graph:
- id: main
  class: Workflow
  requirements:
    - class: InlineJavascriptRequirement

  inputs:
    file_name: string

  outputs: []

  steps:
    step_0:
      run: "#new_file"
      in:
        file_name: file_name
      out: [file]

    step_1:
      run: "#sleep"
      in:
        file: step_0/file
      out: []

    step_2:
      run: "#sleep"
      in:
        file: step_0/file
      out: []

- id: new_file
  class: CommandLineTool
  baseCommand: touch
  inputs:
    file_name:
      type: string
      inputBinding:
        position: 1
  outputs:
    file:
      type: File
      outputBinding:
        glob: $(inputs.file_name)

- id: sleep
  class: CommandLineTool
  baseCommand: ["sleep", "1"]
  inputs:
    file:
      type: File
  outputs: []
