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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFileBase;

/**
 * Represents InitialWorkDirRequirement, this requirements define a list of
 * files and subdirectories that must be created by the workflow platform in the
 * designated output directory prior to executing the command line tool.
 */
@JsonInclude(Include.NON_NULL)
public class InitialWorkDirRequirement extends Requirement {

    private CWLFieldValue listing;
    private List<CWLFileBase> fileListing;
    private List<CWLFileBase> dirListing;
    private List<Dirent> direntListing;
    private List<CWLFieldValue> exprListing;

    /**
     * Returns a CWLExpression object representation of the listing field
     * 
     * @return A CWLExpression object
     */
    public CWLFieldValue getListing() {
        return listing;
    }

    /**
     * Sets a CWLExpression object representation for the listing field
     * 
     * @param listing
     *            A CWLExpression object
     */
    public void setListing(CWLFieldValue listing) {
        this.listing = listing;
    }

    /**
     * Returns a CWLFile list representation of the listing field
     * 
     * @return A list of CWLFile
     */
    public List<CWLFileBase> getFileListing() {
        return fileListing;
    }

    /**
     * Sets a CWLFile list representation for the listing field
     * 
     * @param fileListing
     *            A list of CWLFile
     */
    public void setFileListing(List<CWLFileBase> fileListing) {
        this.fileListing = fileListing;
    }

    /**
     * Returns a CWLDirectory list representation of the listing field
     * 
     * @return A list of CWLDirectory
     */
    public List<CWLFileBase> getDirListing() {
        return dirListing;
    }

    /**
     * Sets a CWLDirectory list representation of the listing field
     * 
     * @param dirListing
     *            A list of CWLDirectory
     */
    public void setDirListing(List<CWLFileBase> dirListing) {
        this.dirListing = dirListing;
    }

    /**
     * Returns a Dirent list representation of the listing field
     * 
     * @return A list of Dirent
     */
    public List<Dirent> getDirentListing() {
        return direntListing;
    }

    /**
     * Sets a Dirent list representation for the listing field
     * 
     * @param direntListing
     *            A list of Dirent
     */
    public void setDirentListing(List<Dirent> direntListing) {
        this.direntListing = direntListing;
    }

    /**
     * Returns a CWLExpression list representation of the listing field
     * 
     * @return A list of CWLExpression
     */
    public List<CWLFieldValue> getExprListing() {
        return exprListing;
    }

    /**
     * Sets a CWLExpression list representation of the listing field
     * 
     * @param exprListing
     *            A list of CWLExpression
     */
    public void setExprListing(List<CWLFieldValue> exprListing) {
        this.exprListing = exprListing;
    }

    /**
     * Always "InitialWorkDirRequirement"
     */
    @Override
    public String getClazz() {
        return "InitialWorkDirRequirement";
    }

}
