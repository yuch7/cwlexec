cwlVersion: v1.0
$graph:
- id: main
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
      run: "untar"
      in:
        tarfile: inp
        extractfile: ex
      out: [example_out]

    compile:
      run: "#compile"
      in:
        src: untar/example_out
      out: [classfile]

- id: untar
  class: CommandLineTool
  baseCommand: [tar, xf]
  inputs:
    tarfile:
      type: File
      inputBinding:
        position: 1
    extractfile:
      type: string
      inputBinding:
        position: 2
  outputs:
    example_out:
      type: File
      outputBinding:
        glob: $(inputs.extractfile)

- id: compile
  class: CommandLineTool
  label: Example trivial wrapper for Java 9 compiler
  baseCommand: javac
  arguments: ["-d", $(runtime.outdir)]
  inputs:
    src:
      type: File
      inputBinding:
        position: 1
  outputs:
    classfile:
      type: File
      outputBinding:
        glob: "*.class"