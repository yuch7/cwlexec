cwlVersion: v1.0
class: Workflow
inputs:
  inp: File
  ex: string

outputs:
  classout:
    type: File
    outputSource: compile/classfile

steps:
  untar:
    run:
      $import: 
        key: value
    in:
      tarfile: inp
      extractfile: ex
    out: [example_out]

  compile:
    run:
      $import: javac.cwl
    in:
      src: untar/example_out
    out: [classfile]