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
package de.dkfz.tbi.otp.administration

import grails.plugin.springsecurity.acl.AclSid
import grails.test.hibernate.HibernateSpec
import grails.testing.web.controllers.ControllerUnitTest
import grails.transaction.Rollback
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import spock.lang.Shared
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.administration.DocumentFactory
import de.dkfz.tbi.otp.repository.administration.document.DocumentDataService
import de.dkfz.tbi.otp.repository.administration.document.DocumentTypeDataService
import de.dkfz.tbi.otp.security.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import static javax.servlet.http.HttpServletResponse.*

class DocumentControllerSpec extends HibernateSpec implements ControllerUnitTest<DocumentController>, UserAndRoles, DocumentFactory {

    @Override
    List<Class> getDomainClasses() {
        return [
                AclSid,
                Document,
                DocumentType,
                Role,
                User,
                UserRole,
        ]
    }

    @Shared
    DocumentDataService documentDataService
    @Shared
    DocumentTypeDataService documentTypeDataService

    @Override
    void setup() {
        documentDataService = hibernateDatastore.getService(DocumentDataService)
        documentTypeDataService = hibernateDatastore.getService(DocumentTypeDataService)
    }

    void setupData() {
        createUserAndRoles()
        controller.documentService = new DocumentService()
        controller.documentService.documentDataService = documentDataService
        controller.documentService.documentTypeDataService = documentTypeDataService
    }

    @Rollback
    void "test upload, successful"() {
        given:
        setupData()
        DocumentType documentType = createDocumentType()
        Document.FormatType formatType = Document.FormatType.CSV
        String content = "ABC"

        when:
        controller.request.method = 'POST'
        controller.params.documentType = documentType
        controller.params.formatType = formatType
        controller.params.content = content.bytes

        SynchronizerTokensHolder tokenHolder = SynchronizerTokensHolder.store(controller.session)
        controller.params[SynchronizerTokensHolder.TOKEN_URI] = '/document/upload'
        controller.params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken("/document/upload")

        controller.upload()

        then:
        controller.response.status == SC_MOVED_TEMPORARILY
        controller.response.redirectedUrl == "/document/manage"
        controller.flash.message.message == "document.store.succ"
        Document d = exactlyOneElement(Document.all)
        d.documentType == documentType
        d.formatType == formatType
        d.content == content.bytes
    }

    @Rollback
    @Unroll
    void "test upload, fails because of missing #problem"() {
        given:
        setupData()
        DocumentType documentType = createDocumentType()
        Document.FormatType formatType = Document.FormatType.CSV
        String content = "ABC"

        when:
        controller.request.method = 'POST'
        controller.params.documentType = documentType
        controller.params.formatType = formatType
        if (problem != "file") {
            controller.params.content = content.bytes
        }

        if (problem != "token") {
            SynchronizerTokensHolder tokenHolder = SynchronizerTokensHolder.store(controller.session)
            controller.params[SynchronizerTokensHolder.TOKEN_URI] = '/document/upload'
            controller.params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken("/document/upload")
        }
        controller.upload()

        then:
        controller.response.status == SC_MOVED_TEMPORARILY
        controller.response.redirectedUrl == "/document/manage"
        controller.flash.message.message == "document.store.fail"
        Document.all.empty

        where:
        problem << ["token", "file"]
    }

    @Rollback
    void "test upload, fails because of wrong method"() {
        given:
        setupData()

        when:
        controller.upload()

        then:
        controller.response.status == SC_METHOD_NOT_ALLOWED
        Document.all.empty
    }

    @Rollback
    void "test download"() {
        given:
        setupData()
        DocumentType documentType = createDocumentType()
        Document.FormatType formatType = Document.FormatType.CSV
        String content = "ABC"

        Document document = createDocument(
            documentType: documentType,
            content: content,
            formatType: formatType,
        )

        when:
        controller.request.method = 'GET'
        controller.params.document = document
        controller.params.to = to

        controller.download()

        then:
        controller.response.status == SC_OK
        controller.response.contentType.startsWith(Document.FormatType.CSV.mimeType)
        controller.response.header("Content-Disposition") == ((to == DocumentController.Action.DOWNLOAD) ?
                "attachment;filename=\"${documentType.title.toLowerCase()}.${formatType.extension}\"" :
                null)
        controller.response.contentAsByteArray == content.bytes

        where:
        to                                 | _
        DocumentController.Action.DOWNLOAD | _
        DocumentController.Action.VIEW     | _
    }

    @Rollback
    void "test manage"() {
        given:
        setupData()
        DocumentType documentType = createDocumentType()
        Document document = createDocument(documentType: documentType)

        when:
        Map model = controller.manage()

        then:
        controller.response.status == SC_OK
        model.documents instanceof Map
        model.documents.get(documentType) == document
        exactlyOneElement(model.documents.keySet()) ==  documentType
    }
}
