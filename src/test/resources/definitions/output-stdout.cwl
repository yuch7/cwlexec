cwlVersion: v1.0
class: CommandLineTool
baseCommand: echo

stdout: a_stdout_file
stderr: a_stderr_file

inputs: []
outputs:
  - id: an_output_name
    type: stdout
  - id: an_errput_name
    type: stderr