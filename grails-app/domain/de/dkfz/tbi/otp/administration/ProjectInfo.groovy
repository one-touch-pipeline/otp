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
package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.Entity

import java.text.DateFormat
import java.text.SimpleDateFormat

class ProjectInfo implements Entity {

    /**
     * Types of legal basis on which data transfers can be executed.
     *
     * Please consult with your local legal expert before adding any new types.
     */
    enum LegalBasis {
        /** A signed, one-off Data Transfer Agreement between the local institute and the recipient. */
        DTA,
        /** Recipient is (acting on behalf of) the subject of the data. */
        PROBAND,
        /** Recipient is involved in a long-term cooperation with the local project PI, perhaps even as sample provider. */
        COOPERATION,
        /** For those gray areas that earn legal departments their paychecks. You probably do not want to use this. */
        UNKNOWN,
    }

    /** filename of the document represented by this instance */
    String fileName

    Project project
    String comment

    /** if this document is a data transfer, under what label is it filed? */
    String dtaId

    /** if this document is a data transfer, with which external party are we transferring? */
    String peerInstitution

    /** if this document is a data transfer, which legal justification does it have? */
    LegalBasis legalBasis

    /** if this document is a data transfer, until when does the usage-right exist? */
    Date validityDate

    Set<DataTransfer> transfers

    static hasMany = [
            transfers: DataTransfer,
    ]

    static constraints = {
        fileName blank: false, unique: 'project', shared: "pathComponent"
        dtaId blank: false, nullable: true
        peerInstitution blank: false, nullable: true
        legalBasis nullable: true
        validityDate nullable: true
        comment blank: false, nullable: true
    }

    static mapping = {
        fileName type: "text"
        comment type: "text"
        transfers lazy: false
    }

    String getPath() {
        return "${project.projectDirectory}/${ProjectService.PROJECT_INFO}/${fileName}"
    }

    /** is this document a Data Transfer Agreement? */
    boolean isDta() {
        // DTA's always need an institute to/from which to transfer data.
        // NOTE: keep this in sync with the Service: ProjectInfoService.createProjectInfoAndUploadFile
        return peerInstitution != null
    }

    List<DataTransfer> getTransfersSortedByDateCreatedDesc() {
        return transfers.sort(ProjectInfoService.SORT_DATE_CREATED_DESC)
    }

    @Override
    String toString() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd" , Locale.ENGLISH)

        String result = "ProjectInfo ${id}, file: ${path}"
        if (peerInstitution) {
            result += [
                    "dtaId: ${dtaId ?: '-'}",
                    "peerInstitution: ${peerInstitution}",
                    "legalBasis: ${legalBasis}",
                    "validityDate: ${validityDate ? dateFormat.format(validityDate) : '-'}",
                    "\nComment: ${comment}",
            ].join(" | ")
        }
        transfers.each {
            result += it.toString()
        }
        return result
    }
}
