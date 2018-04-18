package com.ibm.spectrumcomputing.cwl.model.persistence;

import java.io.Serializable;

import javax.persistence.*;

import org.hibernate.annotations.GenericGenerator;

import com.ibm.spectrumcomputing.cwl.model.RuntimeEnv;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstanceState;

/**
 * Represents the persistence object for a CWL main process instance, one record
 * corresponds with one instance
 */
@Entity
@Table(name = "cwlWorkflow")
public class CWLMainProcessRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id")
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "owner", nullable = false)
    private String owner;

    @Column(name = "descPath", nullable = false)
    private String descPath;

    @Column(name = "inputsPath", nullable = true)
    private String inputsPath;

    @Column(name = "execConfPath", nullable = true)
    private String execConfPath;

    @Column(name = "outputsDir", nullable = true)
    private String outputsDir;

    @Column(name = "workDir", nullable = true)
    private String workDir;

    @Column(name = "runtimeEnv", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private RuntimeEnv runtimeEnv;

    @Column(name = "mainId", nullable = true)
    private String mainId;

    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private CWLInstanceState state;

    @Column(name = "hpcJobId", nullable = false)
    private Long hpcJobId = -1L;

    @Column(name = "exitCode", nullable = false)
    private Integer exitCode = -1;

    @Column(name = "submitTime", nullable = false)
    private Long submitTime;

    @Column(name = "startTime", nullable = true)
    private Long startTime;

    @Column(name = "endTime", nullable = true)
    private Long endTime;

    /**
     * Returns an UUID for this record
     * 
     * @return An UUID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the record ID for this record, it is called by Hibernate framework
     * automatically
     * 
     * @param id
     *            A record ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the name of this record
     * 
     * @return The name of this record
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this record
     * 
     * @param name
     *            The name of this record
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the owner of this record
     * 
     * @return The owner of this record
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Records the owner for corresponding instance
     * 
     * @param owner
     *            The owner of this record
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Returns the corresponding CWL description file path for this record
     * 
     * @return A CWL description file path
     */
    public String getDescPath() {
        return descPath;
    }

    /**
     * Records the CWL description file path for corresponding instance
     * 
     * @param descPath
     *            A CWL description file path
     */
    public void setDescPath(String descPath) {
        this.descPath = descPath;
    }

    /**
     * Returns the corresponding CWL input settings file path for this record
     * 
     * @return A CWL input settings file path
     */
    public String getInputsPath() {
        return inputsPath;
    }

    /**
     * Records the CWL input settings file path for corresponding instance
     * 
     * @param inputsPath
     *            A CWL input settings file path
     */
    public void setInputsPath(String inputsPath) {
        this.inputsPath = inputsPath;
    }

    /**
     * Returns the corresponding execution configuration file path for this
     * record
     * 
     * @return An execution configuration file path
     */
    public String getExecConfPath() {
        return execConfPath;
    }

    /**
     * Records the execution configuration file path for corresponding instance
     * 
     * @param execConfPath
     *            An execution configuration file path
     */
    public void setExecConfPath(String execConfPath) {
        this.execConfPath = execConfPath;
    }

    /**
     * Returns the corresponding output directory path for this record
     * 
     * @return An output directory path
     */
    public String getOutputsDir() {
        return outputsDir;
    }

    /**
     * Records the output directory path for corresponding instance
     * 
     * @param outputsDir
     *            An output directory path
     */
    public void setOutputsDir(String outputsDir) {
        this.outputsDir = outputsDir;
    }

    /**
     * Returns the corresponding working directory path for this record
     * 
     * @return A working directory path
     */
    public String getWorkDir() {
        return workDir;
    }

    /**
     * Records the working directory path for corresponding instance
     * 
     * @param workDir
     *            A working directory path
     */
    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    /**
     * Returns the corresponding runtime environment for this record
     * 
     * @return A runtime environment
     */
    public RuntimeEnv getRuntimeEnv() {
        return runtimeEnv;
    }

    /**
     * Records the runtime environment for corresponding instance
     * 
     * @param runtimeEnv
     *            A runtime environment
     */
    public void setRuntimeEnv(RuntimeEnv runtimeEnv) {
        this.runtimeEnv = runtimeEnv;
    }

    /**
     * If the corresponding instance of this record has graph directive, returns
     * the main process ID for the corresponding instance
     * 
     * @return The main process ID
     */
    public String getMainId() {
        return mainId;
    }

    /**
     * If the corresponding instance of this record has graph directive, records
     * the main process ID for the corresponding instance
     * 
     * @param mainId
     *            The main process ID
     */
    public void setMainId(String mainId) {
        this.mainId = mainId;
    }

    /**
     * Returns the state of this record
     * 
     * @return An instance state
     */
    public CWLInstanceState getState() {
        return state;
    }

    /**
     * Records the state for corresponding instance
     * 
     * @param state
     *            An instance state
     */
    public void setState(CWLInstanceState state) {
        this.state = state;
    }

    /**
     * Returns the HPC job ID for this record
     * 
     * @return A HPC job ID
     */
    public Long getHpcJobId() {
        return hpcJobId;
    }

    /**
     * Records the HPC job ID for corresponding instance
     * 
     * @param hpcJobId
     *            A HPC job ID
     */
    public void setHpcJobId(Long hpcJobId) {
        this.hpcJobId = hpcJobId;
    }

    /**
     * Returns the exit code for this record
     * 
     * @return An exit code
     */
    public Integer getExitCode() {
        return exitCode;
    }

    /**
     * Records the exit code for corresponding instance
     * 
     * @param exitCode
     *            An exit code
     */
    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    /**
     * Returns the submit time for this record
     * 
     * @return A submit time
     */
    public Long getSubmitTime() {
        return submitTime;
    }

    /**
     * Records the submit time for corresponding instance
     * 
     * @param submitTime
     *            A submit time
     */
    public void setSubmitTime(Long submitTime) {
        this.submitTime = submitTime;
    }

    /**
     * Returns the start time for this record
     * 
     * @return A start time
     */
    public Long getStartTime() {
        return startTime;
    }

    /**
     * Records the start time for corresponding instance
     * 
     * @param startTime
     *            A start time
     */
    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    /**
     * Returns the end time for this record
     * 
     * @return An end time
     */
    public Long getEndTime() {
        return endTime;
    }

    /**
     * Records the end time for corresponding instance
     * 
     * @param endTime
     *            An end time
     */
    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }
}
