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
package com.ibm.spectrumcomputing.cwl.model.process;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.Requirement;
import com.ibm.spectrumcomputing.cwl.model.process.tool.CommandLineTool;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.Workflow;

/**
 * Represents a CWL process object, it is the base class for
 * {@link CommandLineTool} and {@link Workflow}
 */
@JsonInclude(Include.NON_NULL)
public abstract class CWLProcess {

    /**
     * A constant string representation of the CWL Workflow class
     */
    public static final String CLASS_WORKFLOW = "Workflow";
    /**
     * A constant string representation of the CWL CommandLineTool class
     */
    public static final String CLASS_COMMANDLINETOOL = "CommandLineTool";
    /**
     * A constant string representation of the CWL ExpressionTool class
     */
    public static final String CLASS_EXPRESSIONTOOL = "ExpressionTool";

    // Use the CWL description file absolute path as the unique identifier
    private String id;
    private List<Requirement> requirements;
    private List<Requirement> hints;
    private String label;
    private String doc;
    private CWLVersion cwlVersion;
    private Map<String, String> namespaces;

    // The owner of CWL description file
    private String owner;
    // The absolute description file path
    private String descPath;
    // The absolute input settings file path
    private String inputsPath;
    // The absolute execution configuration path
    private String execConfPath;
    // If the process has $graph, the graph mainId
    private String mainId;

    /**
     * An abstract method to return the inputs of this process object
     * 
     * @return The inputs of this process object
     */
    public abstract List<? extends CWLParameter> getInputs();

    /**
     * An abstract method to return the outputs of this process object
     * 
     * @return The outputs of this process object
     */
    public abstract List<? extends CWLParameter> getOutputs();

    /**
     * An abstract method to return the class of this process object
     * 
     * @return The class of this process object
     */
    @JsonProperty("class")
    public abstract String getClazz();

    /**
     * Returns the ID of this process object, the ID is CWL description file
     * absolute path
     * 
     * @return The ID of this process object
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of this process object
     * 
     * @param id
     *            The ID of this process object
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the requirements of this process object
     * 
     * @return A list of requirements
     */
    public List<Requirement> getRequirements() {
        return requirements;
    }

    /**
     * Sets the requirements for this process object
     * 
     * @param requirements
     *            A list of requirements
     */
    public void setRequirements(List<Requirement> requirements) {
        this.requirements = requirements;
    }

    /**
     * Returns the hints of this process object
     * 
     * @return A list of hints
     */
    public List<Requirement> getHints() {
        return hints;
    }

    /**
     * Sets the hints for this process object
     * 
     * @param hints
     *            A list of hints
     */
    public void setHints(List<Requirement> hints) {
        this.hints = hints;
    }

    /**
     * Returns a human-readable label of this process object
     * 
     * @return A label of this process object
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets a human-readable label of this process object
     * 
     * @param label
     *            A human-readable label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns a human-readable description of this process object
     * 
     * @return A description of this process object
     */
    public String getDoc() {
        return doc;
    }

    /**
     * Sets a human-readable description of this process object
     * 
     * @param doc
     *            A human-readable description
     */
    public void setDoc(String doc) {
        this.doc = doc;
    }

    /**
     * Returns the CWL document version of this process object
     * 
     * @return A CWL document version
     */
    public CWLVersion getCwlVersion() {
        return cwlVersion;
    }

    /**
     * Sets a CWL document version of this process object
     * 
     * @param cwlVersion
     *            A CWL document version
     */
    public void setCwlVersion(CWLVersion cwlVersion) {
        this.cwlVersion = cwlVersion;
    }

    /**
     * Returns the owner of this process object
     * 
     * @return The owner of this process object
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Sets the owner of CWL document description file for this process object
     * 
     * @param owner
     *            The owner of CWL document description file
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Returns the CWL document description file path for this process object
     * 
     * @return The CWL document description file path
     */
    public String getDescPath() {
        return descPath;
    }

    /**
     * Sets the CWL document description file path for this process object
     * 
     * @param descPath
     *            The CWL document description file path
     */
    public void setDescPath(String descPath) {
        this.descPath = descPath;
    }

    /**
     * Returns the input settings file path for this process object
     * 
     * @return The input settings file path
     */
    public String getInputsPath() {
        return inputsPath;
    }

    /**
     * Sets the input settings file path for this process object
     * 
     * @param inputsPath
     *            The input settings file path
     */
    public void setInputsPath(String inputsPath) {
        this.inputsPath = inputsPath;
    }

    /**
     * Returns the execution configuration file path for this process object
     * 
     * @return The execution configuration file path
     */
    public String getExecConfPath() {
        return execConfPath;
    }

    /**
     * Sets the execution configuration file path for this process object
     * 
     * @param execConfPath
     *            The execution configuration file path
     */
    public void setExecConfPath(String execConfPath) {
        this.execConfPath = execConfPath;
    }

    /**
     * If this process object has a graph directive, returns the main process id
     * 
     * @return An ID of main process
     */
    public String getMainId() {
        return mainId;
    }

    /**
     * If this process object has a graph directive, sets the main process id
     * 
     * @param mainId
     *            An ID of main process
     */
    public void setMainId(String mainId) {
        this.mainId = mainId;
    }

    /**
     * If this process object has a namespace directive, returns the namespace
     * 
     * @return The namespace
     */
    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    /**
     * If this process object has a namespace directive, sets the namespace
     * 
     * @param namespaces
     *            The namespace
     */
    public void setNamespaces(Map<String, String> namespaces) {
        this.namespaces = namespaces;
    }

}
