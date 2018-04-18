cwlVersion: v1.0
$graph:
- id: main
  class: Workflow
  requirements:
    - class: InlineJavascriptRequirement
    - class: SubworkflowFeatureRequirement

  inputs:
    file_name: string

  outputs:
    file:
      type: File
      outputSource: step_2/output_file

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
        message: 
          default: "step 1"
      out: [output_file]

    step_2:
      run: "#append"
      in:
        file: step_1/output_file
        message: 
          default: "step 2"
      out: [output_file]

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

- id: append
  class: CommandLineTool
  baseCommand: sh
  arguments:
    - prefix: -c
      valueFrom: $('cat ' + inputs.file.path + ' > ' + inputs.file.basename +'; echo \"'+ inputs.message + '\" >> ' + inputs.file.basename)
  inputs:
    file:
      type: File
    message:
      type: string
  outputs:
    output_file:
      type: File
      outputBinding:
        glob: $(inputs.file.basename)

- id: sleep
  class: CommandLineTool
  baseCommand: ["sleep", "60"]
  inputs:
    file:
      type: File
    message:
      type: string
  outputs: []
