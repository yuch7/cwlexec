cwlVersion: v1.0
class: CommandLineTool
baseCommand: [test]

requirements:
  - class: InlineJavascriptRequirement

inputs:
  - id: file_path
    type: string
    default: "test0.test1.test2"
outputs:
  - id: output_length
    type: float
    outputBinding:
      glob: "best_frag_length"
      loadContents: True
      outputEval: $(Number(self[0].contents.replace('\n', '')))
  - id: output_sraFiles
    type: File[]
    outputBinding:
      glob: "glob/**"
      outputEval: |
          ${
            var r = [];
            for (var i = 0; i < self.length; i++){
              var run_dirs = self[i].listing;
              if (run_dirs) {
                for (var j = 0; j < run_dirs.length; j++){
                  r.push(run_dirs[j]);
                }
              }
            }
            return r;
          }
  - id: output_path
    type: string
    outputBinding:
      outputEval: $(inputs.file_path.replace(/\.[^/.]+$/, ""))

