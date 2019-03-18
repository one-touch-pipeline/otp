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

import grails.plugin.springsecurity.SpringSecurityUtils
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.springframework.security.access.AccessDeniedException
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.security.UserAndRoles

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import static javax.servlet.http.HttpServletResponse.*

class DocumentControllerIntegrationSpec extends Specification implements UserAndRoles {

    DocumentController controller = new DocumentController()

    def setup() {
        createUserAndRoles()
    }


    void "test upload, successful"() {
        given:
        DocumentType documentType = DomainFactory.createDocumentType()
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

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.upload()
        }

        then:
        controller.response.status == SC_MOVED_TEMPORARILY
        controller.response.redirectedUrl == "/document/manage"
        controller.flash.message.message == "The document was stored successfully"
        Document d = exactlyOneElement(Document.all)
        d.documentType == documentType
        d.formatType == formatType
        d.content == content.bytes
    }

    @Unroll
    void "test upload, fails because of missing #problem"() {
        given:
        DocumentType documentType = DomainFactory.createDocumentType()
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
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.upload()
        }

        then:
        controller.response.status == SC_MOVED_TEMPORARILY
        controller.response.redirectedUrl == "/document/manage"
        controller.flash.message.message == "The document could not be stored"
        Document.all.empty

        where:
        problem << ["token", "file"]
    }

    void "test upload, fails because of wrong method"() {
        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.upload()
        }

        then:
        controller.response.status == SC_METHOD_NOT_ALLOWED
        Document.all.empty
    }

    void "test upload, fails because of wrong permissions"() {
        when:
        controller.request.method = 'POST'
        SynchronizerTokensHolder tokenHolder = SynchronizerTokensHolder.store(controller.session)
        controller.params[SynchronizerTokensHolder.TOKEN_URI] = '/document/upload'
        controller.params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken("/document/upload")

        SpringSecurityUtils.doWithAuth(USER) {
            controller.upload()
        }

        then:
        thrown(AccessDeniedException)
        Document.all.empty
    }

    void "test upload, fails because of missing authentication"() {
        when:
        controller.request.method = 'POST'
        SynchronizerTokensHolder tokenHolder = SynchronizerTokensHolder.store(controller.session)
        controller.params[SynchronizerTokensHolder.TOKEN_URI] = '/document/upload'
        controller.params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken("/document/upload")

        doWithAnonymousAuth {
            controller.upload()
        }

        then:
        thrown(AccessDeniedException)
        Document.all.empty
    }

    void "test download"() {
        given:
        DocumentType documentType = DomainFactory.createDocumentType()
        Document.FormatType formatType = Document.FormatType.CSV
        String content = "ABC"

         Document document = DomainFactory.createDocument(
                documentType: documentType,
                content: content,
                formatType: formatType,
        )

        when:
        controller.params.document = document
        controller.params.to = to

        doWithAnonymousAuth {
            controller.download()
        }

        then:
        controller.response.status == SC_OK
        controller.response.contentType.startsWith(Document.FormatType.CSV.mimeType)
        controller.response.header("Content-Disposition") == ((to == DocumentController.Action.DOWNLOAD) ?
                "attachment;filename=${documentType.title.toLowerCase()}.${formatType.extension}" :
                null)
        controller.response.contentAsByteArray == content.bytes

        where:
        to                                 | _
        DocumentController.Action.DOWNLOAD | _
        DocumentController.Action.VIEW     | _
    }

    void "test manage"() {
        given:
        DocumentType documentType = DomainFactory.createDocumentType()

        Document document = DomainFactory.createDocument(
                documentType: documentType,
        )

        when:
        def model = SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.manage()
        }

        then:
        controller.response.status == SC_OK
        model.documents instanceof Map
        model.documents.get(documentType) == document
        exactlyOneElement(model.documents.keySet()) ==  documentType
    }
}
