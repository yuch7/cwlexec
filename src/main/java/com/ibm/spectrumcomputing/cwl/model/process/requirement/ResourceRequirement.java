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
package com.ibm.spectrumcomputing.cwl.model.process.requirement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;

/**
 * Represents ResourceRequirement
 */
@JsonInclude(Include.NON_NULL)
public class ResourceRequirement extends Requirement {

    private Long coresMin;
    private CWLFieldValue coresMinExpr;
    private Long coresMax;
    private CWLFieldValue coresMaxExpr;
    private Long ramMin;
    private CWLFieldValue ramMinExpr;
    private Long ramMax;
    private CWLFieldValue ramMaxExpr;
    private Long tmpdirMin;
    private CWLFieldValue tmpdirMinExpr;
    private Long tmpdirMax;
    private CWLFieldValue tmpdirMaxExpr;
    private Long outdirMin;
    private CWLFieldValue outdirMinExpr;
    private Long outdirMax;
    private CWLFieldValue outdirMaxExpr;

    /**
     * Returns a long representation of the coresMin
     * 
     * @return A Long object
     */
    public Long getCoresMin() {
        return coresMin;
    }

    /**
     * Sets a long representation for the coresMin
     * 
     * @param coresMin
     *            A Long object
     */
    public void setCoresMin(Long coresMin) {
        this.coresMin = coresMin;
    }

    /**
     * Returns a CWLExpression object representation of the coresMin
     * 
     * @return A CWLExpression object
     */
    public CWLFieldValue getCoresMinExpr() {
        return coresMinExpr;
    }

    /**
     * Sets a CWLExpression object representation for the coresMin
     * 
     * @param coresMinExpr
     *            A CWLExpression object
     */
    public void setCoresMinExpr(CWLFieldValue coresMinExpr) {
        this.coresMinExpr = coresMinExpr;
    }

    /**
     * Returns a long representation of the coresMax
     * 
     * @return A Long object
     */
    public Long getCoresMax() {
        return coresMax;
    }

    /**
     * Sets a long representation for the coresMax
     * 
     * @param coresMax
     *            A Long object
     */
    public void setCoresMax(Long coresMax) {
        this.coresMax = coresMax;
    }

    /**
     * Returns a CWLExpression object representation of the coresMax
     * 
     * @return A CWLExpression object
     */
    public CWLFieldValue getCoresMaxExpr() {
        return coresMaxExpr;
    }

    /**
     * Sets a CWLExpression object representation for the coresMax
     * 
     * @param coresMaxExpr
     *            A CWLExpression object
     */
    public void setCoresMaxExpr(CWLFieldValue coresMaxExpr) {
        this.coresMaxExpr = coresMaxExpr;
    }

    /**
     * Returns a long representation of the ramMin
     * 
     * @return A Long object
     */
    public Long getRamMin() {
        return ramMin;
    }

    /**
     * Sets a long representation for the ramMin
     * 
     * @param ramMin
     *            A Long object
     */
    public void setRamMin(Long ramMin) {
        this.ramMin = ramMin;
    }

    /**
     * Returns a CWLExpression object representation of the ramMin
     * 
     * @return A CWLExpression object
     */
    public CWLFieldValue getRamMinExpr() {
        return ramMinExpr;
    }

    /**
     * Sets a CWLExpression object representation for the ramMin
     * 
     * @param ramMinExpr
     *            A CWLExpression object
     */
    public void setRamMinExpr(CWLFieldValue ramMinExpr) {
        this.ramMinExpr = ramMinExpr;
    }

    /**
     * Returns a long representation of the ramMax
     * 
     * @return A Long object
     */
    public Long getRamMax() {
        return ramMax;
    }

    /**
     * Sets a long representation for the ramMax
     * 
     * @param ramMax
     *            A Long object
     */
    public void setRamMax(Long ramMax) {
        this.ramMax = ramMax;
    }

    /**
     * Returns a CWLExpression object representation of the ramMax
     * 
     * @return A CWLExpression object
     */
    public CWLFieldValue getRamMaxExpr() {
        return ramMaxExpr;
    }

    /**
     * Sets a CWLExpression object representation for the ramMax
     * 
     * @param ramMaxExpr
     *            A CWLExpression object
     */
    public void setRamMaxExpr(CWLFieldValue ramMaxExpr) {
        this.ramMaxExpr = ramMaxExpr;
    }

    /**
     * Returns a long representation of the tmpdirMin
     * 
     * @return A Long object
     */
    public Long getTmpdirMin() {
        return tmpdirMin;
    }

    /**
     * Sets a long representation for the tmpdirMin
     * 
     * @param tmpdirMin
     *            A Long object
     */
    public void setTmpdirMin(Long tmpdirMin) {
        this.tmpdirMin = tmpdirMin;
    }

    /**
     * Returns a CWLExpression object representation of the tmpdirMin
     * 
     * @return A CWLExpression object
     */
    public CWLFieldValue getTmpdirMinExpr() {
        return tmpdirMinExpr;
    }

    /**
     * Sets a CWLExpression object representation for the tmpdirMin
     * 
     * @param tmpdirMinExpr
     *            A CWLExpression object
     */
    public void setTmpdirMinExpr(CWLFieldValue tmpdirMinExpr) {
        this.tmpdirMinExpr = tmpdirMinExpr;
    }

    /**
     * Returns a long representation of the tmpdirMax
     * 
     * @return A Long object
     */
    public Long getTmpdirMax() {
        return tmpdirMax;
    }

    /**
     * Sets a long representation for the tmpdirMax
     * 
     * @param tmpdirMax
     *            A Long object
     */
    public void setTmpdirMax(Long tmpdirMax) {
        this.tmpdirMax = tmpdirMax;
    }

    /**
     * Returns a CWLExpression object representation of the tmpdirMax
     * 
     * @return A CWLExpression object
     */
    public CWLFieldValue getTmpdirMaxExpr() {
        return tmpdirMaxExpr;
    }

    /**
     * Sets a CWLExpression object representation for the tmpdirMax
     * 
     * @param tmpdirMaxExpr
     *            A CWLExpression object
     */
    public void setTmpdirMaxExpr(CWLFieldValue tmpdirMaxExpr) {
        this.tmpdirMaxExpr = tmpdirMaxExpr;
    }

    /**
     * Returns a long representation of the outdirMin
     * 
     * @return A Long object
     */
    public Long getOutdirMin() {
        return outdirMin;
    }

    /**
     * Sets a long representation for the outdirMin
     * 
     * @param outdirMin
     *            A Long object
     */
    public void setOutdirMin(Long outdirMin) {
        this.outdirMin = outdirMin;
    }

    /**
     * Returns a CWLExpression object representation of the outdirMin
     * 
     * @return A CWLExpression object
     */
    public CWLFieldValue getOutdirMinExpr() {
        return outdirMinExpr;
    }

    /**
     * Sets a CWLExpression object representation for the outdirMin
     * 
     * @param outdirMinExpr
     *            A CWLExpression object
     */
    public void setOutdirMinExpr(CWLFieldValue outdirMinExpr) {
        this.outdirMinExpr = outdirMinExpr;
    }

    /**
     * Returns a long representation of the outdirMax
     * 
     * @return A Long object
     */
    public Long getOutdirMax() {
        return outdirMax;
    }

    /**
     * Sets a long representation for the outdirMax
     * 
     * @param outdirMax
     *            A Long object
     */
    public void setOutdirMax(Long outdirMax) {
        this.outdirMax = outdirMax;
    }

    /**
     * Returns a CWLExpression object representation of the outdirMax
     * 
     * @return A CWLExpression object
     */
    public CWLFieldValue getOutdirMaxExpr() {
        return outdirMaxExpr;
    }

    /**
     * Sets a CWLExpression object representation for the outdirMax
     * 
     * @param outdirMaxExpr
     *            A CWLExpression object
     */
    public void setOutdirMaxExpr(CWLFieldValue outdirMaxExpr) {
        this.outdirMaxExpr = outdirMaxExpr;
    }

    /**
     * Always "ResourceRequirement"
     */
    @Override
    public String getClazz() {
        return "ResourceRequirement";
    }

}
