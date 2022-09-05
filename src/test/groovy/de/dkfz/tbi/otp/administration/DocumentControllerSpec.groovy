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

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.administration.DocumentFactory
import de.dkfz.tbi.otp.security.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import static javax.servlet.http.HttpServletResponse.*

class DocumentControllerSpec extends Specification implements ControllerUnitTest<DocumentController>, DataTest, UserAndRoles, DocumentFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Document,
                DocumentType,
                User,
                UserRole,
                Role,
        ]
    }

    void setupData() {
        createUserAndRoles()
        controller.documentService = new DocumentService()
    }

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

    void "test upload, fails because of wrong method"() {
        given:
        setupData()

        when:
        controller.upload()

        then:
        controller.response.status == SC_METHOD_NOT_ALLOWED
        Document.all.empty
    }

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
