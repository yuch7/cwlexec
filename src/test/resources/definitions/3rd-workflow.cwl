cwlVersion: v1.0
$graph:
- id: main
  class: Workflow
  requirements:
    - class: InlineJavascriptRequirement
    - class: StepInputExpressionRequirement
    - class: SubworkflowFeatureRequirement

  inputs:
    file_name: string

  outputs:
    file:
      type: File
      outputSource: step_3/output_file

  steps:
    step_0:
      run: "#new_file"
      in:
        file_name: file_name
      out: [file]

    step_1:
      run: "#append"
      in:
        file: step_0/file
        message: 
          default: "step 1"
      out: [output_file]

    step_2:
      run: "#sub_workflow_1"
      in:
        file: step_1/output_file
        prefix: 
          default: "step 2"
      out: [output_file]

    step_3:
      run: "#append"
      in:
        file: step_2/output_file
        message: 
          default: "step 3"
      out: [output_file]

- id: sub_workflow_1
  class: Workflow
  requirements:
    - class: InlineJavascriptRequirement

  inputs:
    prefix: string
    file: File

  outputs:
    output_file:
      type: File
      outputSource: step_C/output_file

  steps:
    step_A:
      run: "#append"
      in:
        file: file
        message:
          source: prefix
          valueFrom: $(self + '->' + 'step A')
      out: [output_file]

    step_B:
      run: "#sub_workflow_2"
      in:
        file: step_A/output_file
        prefix: 
          source: prefix
          valueFrom: $(self + '->' + 'step B')
      out: [output_file]
      
    step_C:
      run: "#append"
      in:
        file: step_B/output_file
        message: 
          source: prefix
          valueFrom: $(self + '->' + 'step C')
      out: [output_file]

- id: sub_workflow_2
  class: Workflow
  requirements:
    - class: InlineJavascriptRequirement

  inputs:
    prefix: string
    file: File

  outputs:
    output_file:
      type: File
      outputSource: step_b/output_file

  steps:
    step_a:
      run: "#append"
      in:
        file: file
        message:
          source: prefix
          valueFrom: $(self + '->' + 'step a')
      out: [output_file]

    step_b:
      run: "#append"
      in:
        file: step_a/output_file
        message: 
          source: prefix
          valueFrom: $(self + '->' + 'step b')
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