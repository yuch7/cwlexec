/*
 * Copyright 2002-2012 the original author or authors.
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
package com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents a CWLFile object
 */
@JsonInclude(Include.NON_NULL)
public class CWLFile extends CWLFileBase {

    private String dirname;
    private String nameroot;
    private String nameext;
    private String checksum;
    private long size;
    private List<CWLFileBase> secondaryFiles;
    private String format;
    private String contents;

    /**
     * Returns the name of the directory containing file, that is, the path
     * leading up to the final slash in the path such that dirname + '/' +
     * basename == path.
     * 
     * @return The name of the directory containing file
     */
    public String getDirname() {
        return dirname;
    }

    /**
     * Sets the name of the directory containing file, that is, the path leading
     * up to the final slash in the path such that dirname + '/' + basename ==
     * path.
     * 
     * @param dirname
     *            The name of the directory containing file
     */
    public void setDirname(String dirname) {
        this.dirname = dirname;
    }

    /**
     * Returns the basename root such that nameroot + nameext == basename, and
     * nameext is empty or begins with a period and contains at most one period.
     * For the purposess of path splitting leading periods on the basename are
     * ignored; a basename of .cshrc will have a nameroot of .cshrc.
     * 
     * @return The root name of a file
     */
    public String getNameroot() {
        return nameroot;
    }

    /**
     * Sets the basename root such that nameroot + nameext == basename, and
     * nameext is empty or begins with a period and contains at most one period.
     * For the purposess of path splitting leading periods on the basename are
     * ignored; a basename of .cshrc will have a nameroot of .cshrc.
     * 
     * @param nameroot
     *            The root name of a file
     */
    public void setNameroot(String nameroot) {
        this.nameroot = nameroot;
    }

    /**
     * Returns the basename extension such that nameroot + nameext == basename,
     * and nameext is empty or begins with a period and contains at most one
     * period. Leading periods on the basename are ignored; a basename of .cshrc
     * will have an empty nameext.
     * 
     * @return The extension name of a file
     */
    public String getNameext() {
        return nameext;
    }

    /**
     * Sets the basename extension such that nameroot + nameext == basename, and
     * nameext is empty or begins with a period and contains at most one period.
     * Leading periods on the basename are ignored; a basename of .cshrc will
     * have an empty nameext.
     * 
     * @param nameext
     *            The extension name of a file
     */
    public void setNameext(String nameext) {
        this.nameext = nameext;
    }

    /**
     * Returns a hash code for a file it is in the form "sha1$ + hexadecimal
     * string" using the SHA-1 algorithm.
     * 
     * @return A hash code of a file
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Sets a hash code for a file it is in the form "sha1$ + hexadecimal
     * string" using the SHA-1 algorithm.
     * 
     * @param checksum
     *            A hash code of a file
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    /**
     * Returns the file size
     * 
     * @return The file size
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets the file size
     * 
     * @param size
     *            The file size
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Returns a list of additional files or directories that are associated
     * with the primary file and must be transferred alongside the primary file.
     * 
     * @return A list of additional files or directories
     */
    public List<CWLFileBase> getSecondaryFiles() {
        return secondaryFiles;
    }

    /**
     * Sets a list of additional files or directories that are associated with
     * the primary file and must be transferred alongside the primary file.
     * 
     * @param secondaryFiles
     *            A list of additional files or directories
     */
    public void setSecondaryFiles(List<CWLFileBase> secondaryFiles) {
        this.secondaryFiles = secondaryFiles;
    }

    /**
     * Returns the format of a file: this must be an IRI of a concept node that
     * represents the file format, preferrably defined within an ontology. If no
     * ontology is available, file formats may be tested by exact match.
     * 
     * @return The format of a file
     */
    public String getFormat() {
        return format;
    }

    /**
     * Sets the format of a file: this must be an IRI of a concept node that
     * represents the file format, preferrably defined within an ontology. If no
     * ontology is available, file formats may be tested by exact match.
     * 
     * @param format
     *            The format of a file
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Returns file contents literal. Maximum of 64 KiB.
     * 
     * @return The contents of a file
     */
    public String getContents() {
        return contents;
    }

    /**
     * Sets file contents literal. Maximum of 64 KiB.
     * 
     * @param contents
     *            The contents of a file
     */
    public void setContents(String contents) {
        this.contents = contents;
    }

    /**
     * Always "File"
     */
    @Override
    public String getClazz() {
        return "File";
    }
}
