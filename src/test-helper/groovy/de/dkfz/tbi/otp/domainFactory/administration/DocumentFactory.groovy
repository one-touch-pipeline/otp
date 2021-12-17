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
package de.dkfz.tbi.otp.domainFactory.administration

import org.springframework.mock.web.MockMultipartFile

import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.project.ProjectInfo
import de.dkfz.tbi.otp.project.dta.DataTransfer
import de.dkfz.tbi.otp.project.dta.DataTransferAgreement
import de.dkfz.tbi.otp.project.dta.DataTransferAgreementDocument
import de.dkfz.tbi.otp.project.dta.DataTransferDocument
import de.dkfz.tbi.otp.utils.HelperUtils

trait DocumentFactory implements DomainFactoryCore {

    Document createDocument(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(Document, [
                content     : HelperUtils.uniqueString.bytes,
                formatType  : Document.FormatType.PDF,
                documentType: { createDocumentType() },
        ], properties, saveAndValidate)
    }

    DocumentType createDocumentType(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(DocumentType, [
                title      : "title${nextId}",
                description: 'description',
        ], properties, saveAndValidate)
    }

    ProjectInfo createProjectInfo(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(ProjectInfo, [
                fileName: "fileName_${nextId}",
                project : { createProject() },
        ], properties, saveAndValidate)
    }

    DataTransfer createDataTransfer(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(DataTransfer, [
                requester     : "requester_${nextId}",
                performingUser: { DomainFactory.createUser() },
                direction     : DataTransfer.Direction.OUTGOING,
                transferMode  : DataTransfer.TransferMode.ASPERA,
                peerPerson    : "peerPerson_${nextId}",
                transferDate  : new Date(),
                dataTransferAgreement: { createDataTransferAgreement() },
        ], properties, saveAndValidate)
    }

    DataTransferDocument createDataTransferDocument(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(DataTransferDocument, [
                fileName: "transfer-file_${nextId}",
                dataTransfer: { createDataTransfer() },
        ], properties, saveAndValidate)
    }

    DataTransferAgreement createDataTransferAgreement(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(DataTransferAgreement, [
                project: { createProject() },
                peerInstitution: "demo institution",
                legalBasis: DataTransferAgreement.LegalBasis.DTA,
                validityDate: new Date(),
                comment: "this is a test comment",
        ], properties, saveAndValidate)
    }

    DataTransferAgreementDocument createDataTransferAgreementDocument(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(DataTransferAgreementDocument, [
                fileName: "dta-file_${nextId}",
                dataTransferAgreement: { createDataTransferAgreement() },
        ], properties, saveAndValidate)
    }

    MockMultipartFile createMultipartFile(String name = "fileName", String originalFilename = "fileName", byte[] content = 0..3) {
        MockMultipartFile mockMultipartFile = new MockMultipartFile(name, content)
        mockMultipartFile.originalFilename = originalFilename
        return mockMultipartFile
    }
}
