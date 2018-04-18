cwlVersion: v1.0
class: Workflow

requirements:
    - class: StepInputExpressionRequirement
    - class: InlineJavascriptRequirement
    - class: MultipleInputFeatureRequirement

inputs:
    input1:
        type: string
        default: "one"
    input2:
        type: string
        default: "two"
outputs: []

steps:
    step1:
        run: echo.cwl
        in:
            msg:
                valueFrom: "three"
        out: []
    step2:
        run: echo.cwl
        in:
            msg:
                source: ["input1", "input2"]
                valueFrom: $(self[0])
        out: []
    step3:
        run: echo.cwl
        in:
            msg:
                source: ["input2"]
                valueFrom: $("other_" + inputs.msg)
        out: []

