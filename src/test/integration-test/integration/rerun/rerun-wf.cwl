cwlVersion: v1.0
class: Workflow
inputs:
  inp: File
  ex: string

outputs:
  classout:
    type: File
    outputSource: step3/append_file

steps:
  step1:
    run: step1.cwl
    in:
      tarfile: inp
      extractfile: ex
    out: [tar_out]

  step2:
    run: step2.cwl
    in:
      src: step1/tar_out
    out: [cat_file]

  step3:
    run: step3.cwl
    in:
      src: step2/cat_file
    out: [append_file]
