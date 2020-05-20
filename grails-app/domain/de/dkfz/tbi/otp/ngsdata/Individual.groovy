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

import de.dkfz.tbi.otp.CommentableWithProject
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

/*
 * In the GUI and e-mails sent by OTP this shall be called "PID".
 * (Decided together with the OTP Product Owner on 2016-07-19.)
 */
/** This table is used externally. Please discuss a change in the team */
class Individual implements CommentableWithProject, Entity {

    /**
     * Identifier used in the file system. It should never change.
     * Usually its the same as {@link #mockPid}
     *
     * This attribute is used externally. Please discuss a change in the team
     */
    String pid

    // TODO OTP-1225: Field names pid, mockPid and mockFullName are confusing and used inconsistently.

    /**
     * Identifier used in the user interface.
     *
     * Access this via {@link #getDisplayName()}
     */
    String mockFullName

    /**
     * Alternative Identifier. If an individual is renamed, this column can contain the old identifier.
     * Also it can be used for visible mapping of identifier.
     */
    String mockPid

    /**
     * Holds an internal identifier, which shouldn't be visible for normal users.
     * Only admin users are allowed to see this value.
     */
    String internIdentifier

    enum Type { REAL, POOL, CELLLINE, UNDEFINED }
    Type type

    /** This attribute is used externally. Please discuss a change in the team */
    Project project

    static belongsTo = [
            project : Project,
    ]

    static constraints = {
        pid(unique: true, nullable: false, blank: false, shared: "pathComponent")
        internIdentifier(nullable: true)
        comment(nullable: true)
    }

    @Override
    String toString() {
        "${mockPid}"
    }

    String getDisplayName() {
        return mockFullName  // TODO: OTP-1225
    }

    /**
     * @return List of Sample for this Individual
     */
    List<Sample> getSamples() {
        return Sample.findAllByIndividual(this)
    }

    /**
     * @return List of SeqType for this Individual
     */
    List<SeqType> getSeqTypes() {
        return SeqType.executeQuery("""
SELECT DISTINCT type from SeqScan scan
INNER JOIN scan.sample AS sample
INNER JOIN scan.seqType as type
WHERE sample.individual = :ind
order by type.name asc, type.libraryLayout
        """, [ind: this])
    }

    List<SeqTrack> getSeqTracks() {
        return SeqTrack.createCriteria().list {
            sample {
                eq("individual", this)
            }
        }
    }

    /**
     * returns the folder viewByPid without the pid
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid
     */
    OtpPath getViewByPidPathBase(final SeqType seqType) {
        return new OtpPath(project, project.dirName, 'sequencing', seqType.dirName, 'view-by-pid')
    }

    /**
     * returns the folder viewByPid with the pid
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}
     */
    OtpPath getViewByPidPath(final SeqType seqType) {
        return new OtpPath(getViewByPidPathBase(seqType), pid)
    }

    /**
     * Example: ${project}/results_per_pid/${pid}
     */
    OtpPath getResultsPerPidPath() {
        return new OtpPath(project, project.dirName, 'results_per_pid', pid)
    }

    @Override
    Project getProject() {
        return project
    }

    static mapping = {
        project index: "individual_project_idx"
        pid index: "individual_pid_idx"
        mockPid index: "individual_mock_pid_idx"
        mockFullName index: "individual_mock_full_name_idx"
    }
}
