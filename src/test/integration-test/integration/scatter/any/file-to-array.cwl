cwlVersion: v1.0
class: CommandLineTool

inputs:
  inputFile: File

baseCommand: python
arguments:
 - prefix: -c
   valueFrom: |
     import json
     fileString = []
     with open("$(inputs.inputFile.path)", "r") as inputFile:
          for line in inputFile:
               fileString.append(line)
     with open("cwl.output.json", "w") as output:
         json.dump({"fileString": fileString}, output)

outputs:
  fileString: string[]
