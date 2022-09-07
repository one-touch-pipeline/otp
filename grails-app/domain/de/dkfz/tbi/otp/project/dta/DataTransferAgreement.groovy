/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.project.dta

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.project.ProjectInfoService
import de.dkfz.tbi.otp.utils.Entity

import de.dkfz.tbi.otp.project.Project

@ManagedEntity
class DataTransferAgreement implements Entity {

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
        /** For EGA submissions, authorizing the upload of the data to EGA */
        REPOSITORY,
        /** For those gray areas that earn legal departments their paychecks. You probably do not want to use this. */
        UNKNOWN,
    }

    Project project

    String comment

    /** Under what label is it filed? */
    String dtaId

    /** With which external party are we transferring? */
    String peerInstitution

    /** Legal justification */
    LegalBasis legalBasis

    /** Until when does the usage-right exist? */
    Date validityDate

    Set<DataTransferAgreementDocument> dataTransferAgreementDocuments

    Set<DataTransfer> transfers

    static hasMany = [
            dataTransferAgreementDocuments: DataTransferAgreementDocument,
            transfers: DataTransfer,
    ]

    static belongsTo = [
            project: Project,
    ]

    static Closure mapping = {
        project index: "data_transfer_agreement_project_idx"
        comment type: "text"
        dataTransferAgreementDocuments lazy: false
        transfers lazy: false
    }

    static mappedBy = [
            dataTransferAgreementDocuments: "dataTransferAgreement",
            transfers: "dataTransferAgreement",
    ]

    static Closure constraints = {
        comment blank: false, nullable: true
        dtaId blank: false, nullable: true
        peerInstitution blank: false
        validityDate nullable: true
    }

    List<DataTransfer> getTransfersSortedByDateCreatedDesc() {
        return transfers.sort(ProjectInfoService.SORT_DATE_CREATED_DESC)
    }

    @Override
    String toString() {
        return "DataTransferAgreement(" +
                "id=${id}" +
                ", project=${project}" +
                ", comment='${comment}'" +
                ", dtaId='${dtaId}'" +
                ", peerInstitution='${peerInstitution}'" +
                ", legalBasis=${legalBasis}" +
                ", validityDate=${validityDate}" +
                ", dataTransferAgreementDocuments=${dataTransferAgreementDocuments}" +
                ", transfers=${transfers}" +
                ")"
    }
}
