#!/usr/bin/env cwl-runner
cwlVersion: v1.0
class: ExpressionTool

requirements:
- class: InlineJavascriptRequirement

inputs:
  datafile:
    type: File
    inputBinding:
      loadContents: true

outputs:
  processedoutput:
    type: Any

expression: "${var lines = inputs.datafile.contents.split('\\n');
               var nblines = lines.length;
               var arrayofarrays = [];
               var setofarrays = {};
               for (var i = 0; i < nblines; i++) {
                  arrayofarrays.push(lines[i].split(','));
                  setofarrays[i] = lines[i].split(',');}
               return { 'processedoutput': setofarrays } ;
              }"

