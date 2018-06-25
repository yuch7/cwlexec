/*
 * Copyright International Business Machines Corp, 2018.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.spectrumcomputing.cwl.exec.util.outputs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstance;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLTypeSymbol;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFileBase;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.DockerRequirement;
import com.ibm.spectrumcomputing.cwl.parser.CWLParser;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;

/**
 * Utility methods for parsing cwl.output.json
 */
public class CWLOutputJsonParser {

    private static final String VAR_SPOOL_CWL = "/var/spool/cwl";

    private CWLOutputJsonParser() {
    }

    /**
     * Parses the cwl.output.json file of a CWL process instance to JSON
     * 
     * @param instance
     *            A CWL process instance
     * @param cwlOutputJsonPath
     *            The path of the cwl.output.json
     * @return The parsed JSON node object
     * @throws CWLException
     *             Failed to parse the cwl.output.json
     * @throws IOException
     *             Failed to parse the cwl.output.json
     */
    public static JsonNode parseCWLOutputJson(CWLInstance instance, Path cwlOutputJsonPath)
            throws CWLException, IOException {
        JsonNode jsonNode = IOUtil.toJsonNode(cwlOutputJsonPath.toFile());
        DockerRequirement dockerRequirement = CWLExecUtil.findRequirement(instance, DockerRequirement.class);
        String tmpDir = instance.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR);
        String outDir = instance.getRuntime().get(CommonUtil.RUNTIME_OUTPUT_DIR);
        ObjectMapper mapper = new ObjectMapper();
        List<? extends CWLParameter> outputs = instance.getProcess().getOutputs();
        for (CWLParameter output : outputs) {
            JsonNode outputNode = jsonNode.get(output.getId());
            if (output.getType() == null || output.getType().getType() == null || outputNode == null) {
                continue;
            }
            CWLTypeSymbol typeSymbol = output.getType().getType().getSymbol();
            if (CWLTypeSymbol.FILE.equals(typeSymbol)) {
                // map the path of file/directory in docker to path out
                // of docker if necessary
                if (dockerRequirement != null) {
                    mapDockerFilePath(instance, outputNode);
                }
                CWLFileBase fileBase = CWLParser.transformCWLFileNode(cwlOutputJsonPath.getParent().toString(),
                        output.getId(),
                        outputNode);
                copyCWLOutputJsonFile(instance, mapper, jsonNode, tmpDir, outDir, output, fileBase);
            } else if (CWLTypeSymbol.DIRECTORY.equals(typeSymbol)) {
                // map the path of file/directory in docker to path out
                // of docker if necessary
                if (dockerRequirement != null) {
                    mapDockerFilePath(instance, outputNode);
                }
                CWLFileBase fileBase = CWLParser.transformCWLDirectoryNode(cwlOutputJsonPath.getParent().toString(),
                        output.getId(),
                        mapDockerFilePath(instance, jsonNode.get(output.getId())));
                copyCWLOutputJsonFile(instance, mapper, jsonNode, tmpDir, outDir, output, fileBase);
            }
        }
        return jsonNode;
    }

    private static void copyCWLOutputJsonFile(CWLInstance instance,
            ObjectMapper mapper,
            JsonNode jsonNode,
            String tmpDir,
            String outDir,
            CWLParameter output,
            CWLFileBase fileBase) throws CWLException {
        // copy file/directory to out dir
        String path = fileBase.getPath();
        if (path != null && path.startsWith(IOUtil.FILE_PREFIX)) {
            path = path.substring(7);
        }
        Path sourcePath = Paths.get(path);
        Path targetPath = Paths.get(instance.getRuntime().get(CommonUtil.RUNTIME_OUTPUT_DIR) + File.separator
                + sourcePath.getFileName());
        IOUtil.copy(instance.getOwner(), sourcePath, targetPath);
        // update path of file/directory to out dir
        if (fileBase.getPath() != null) {
            fileBase.setPath(fileBase.getPath().replace(tmpDir, outDir));
        }
        if (fileBase.getLocation() != null) {
            fileBase.setLocation(fileBase.getLocation().replace(tmpDir, outDir));
        }
        if (fileBase instanceof CWLFile) {
            CWLFile cwlFile = (CWLFile) fileBase;
            if (cwlFile.getDirname() != null) {
                cwlFile.setDirname(cwlFile.getDirname().replace(tmpDir, outDir));
            }
        }
        // update output json node
        if (jsonNode instanceof ObjectNode) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            objectNode.set(output.getId(), mapper.convertValue(fileBase, JsonNode.class));
        }
    }

    private static JsonNode mapDockerFilePath(CWLInstance instance, JsonNode jsonNode) {
        if (jsonNode == null || !(jsonNode instanceof ObjectNode)) {
            return jsonNode;
        }
        ObjectNode objectNode = (ObjectNode) jsonNode;
        String tmpDir = instance.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR);
        JsonNode pathNode = objectNode.get("path");
        if (pathNode != null && pathNode.isTextual() && pathNode.asText() != null
                && pathNode.asText().indexOf(VAR_SPOOL_CWL) != -1) {
            objectNode.put("path", pathNode.asText().replace(VAR_SPOOL_CWL, tmpDir));
        }
        JsonNode locationNode = objectNode.get("location");
        if (locationNode != null && locationNode.isTextual() && locationNode.asText() != null
                && locationNode.asText().indexOf(VAR_SPOOL_CWL) != -1) {
            objectNode.put("location", locationNode.asText().replace(VAR_SPOOL_CWL, tmpDir));
        }
        return jsonNode;
    }

}
