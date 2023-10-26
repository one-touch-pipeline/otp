/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.CommentableWithProject
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

/**
 * In the GUI and e-mails sent by OTP this shall be called "Patient ID".
 * Decided by the OTP Product Owner on 2020-08-20.
 */
/** This table is used externally. Please discuss a change in the team */
@ManagedEntity
class Individual implements CommentableWithProject, Entity {

    /**
     * Patient ID
     *
     * Used in the file system, so it should never change without DataSwap
     *
     * This attribute is used externally. Please discuss a change in the team
     */
    String pid

    UUID uuid = UUID.randomUUID()

    enum Type { REAL, POOL, CELLLINE, UNDEFINED }
    Type type

    SpeciesWithStrain species

    /** This attribute is used externally. Please discuss a change in the team */
    Project project

    static belongsTo = [
            project : Project,
    ]

    static constraints = {
        pid(unique: true, nullable: false, blank: false, shared: "pathComponent")
        uuid(unique: true, nullable: false)
        species(nullable: true)
        comment(nullable: true)
    }

    @Override
    String toString() {
        return pid
    }

    String getDisplayName() {
        return pid
    }

    /**
     * returns the folder viewByPid without the pid
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid
     */
    @Deprecated
    OtpPath getViewByPidPathBase(final SeqType seqType) {
        return new OtpPath(project, project.dirName, 'sequencing', seqType.dirName, 'view-by-pid')
    }

    /**
     * returns the folder viewByPid with the pid
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}
     */
    @Deprecated
    OtpPath getViewByPidPath(final SeqType seqType) {
        return new OtpPath(getViewByPidPathBase(seqType), pid)
    }

    @Override
    Project getProject() {
        return project
    }

    static mapping = {
        project index: "individual_project_idx"
        pid index: "individual_pid_idx"
        uuid index: "individual_uuid_idx"
        comment cascade: "all-delete-orphan"
        species index: "individual_species_idx"
    }
}
