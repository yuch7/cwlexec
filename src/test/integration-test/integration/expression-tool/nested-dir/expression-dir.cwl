class: ExpressionTool
cwlVersion: v1.0
inputs:
  inputdir: Directory
outputs:
  out: Directory[]
requirements:
  InlineJavascriptRequirement: {}
expression: |
  ${
    var samples = {};
    for (var i = 0; i < inputs.inputdir.listing.length; i++) {
      var file = inputs.inputdir.listing[i];
      var groups = file.basename.match(/^(.+)(_R[12]_)(.+)$/);
      if (groups) {
        if (!samples[groups[1]]) {
          samples[groups[1]] = [];
        }
        samples[groups[1]].push(file);
      }
    }
    var dirs = [];
    for (var key in samples) {
      dirs.push({"class": "Directory",
                 "basename": key,
                 "listing": samples[key]});
    }
    return {"out": dirs};
  }
