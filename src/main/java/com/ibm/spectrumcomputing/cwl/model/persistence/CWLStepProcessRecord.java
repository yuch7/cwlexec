package com.ibm.spectrumcomputing.cwl.model.persistence;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import com.ibm.spectrumcomputing.cwl.model.RuntimeEnv;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstanceState;

/**
 * Represents the persistence object for a CWL step process instance, one record
 * corresponds with one instance
 */
@Entity
@Table(name = "cwlStep")
public class CWLStepProcessRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id")
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "parentId", nullable = false)
    private String parentId;

    @Column(name = "owner", nullable = false)
    private String owner;

    @Column(name = "runtimeEnv", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private RuntimeEnv runtimeEnv;

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
     * Returns the parent ID for this record, the parent ID is the
     * {@link CWLMainProcessRecord#getId()}
     * 
     * @return The parent ID
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * Sets the parent ID for this record
     * 
     * @param parentId
     *            The parent ID
     */
    public void setParentId(String parentId) {
        this.parentId = parentId;
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
