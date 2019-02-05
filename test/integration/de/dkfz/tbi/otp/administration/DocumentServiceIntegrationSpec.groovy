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
import grails.test.spock.IntegrationSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.HelperUtils

class DocumentServiceIntegrationSpec extends IntegrationSpec implements UserAndRoles {

    private static final String TITLE = "title"
    private static final String DESCRIPTION = "description"

    void setup() {
        createUserAndRoles()
    }

    @Autowired
    DocumentService service

    void "test createDocumentType, valid input succeeds"() {
        given:
        Errors errors

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            errors = service.createDocumentType(TITLE, DESCRIPTION)
        }

        then:
        DocumentType.findAllByTitleAndDescription(TITLE, DESCRIPTION)
        !errors
    }

    void "test createDocumentType, invalid input fails"() {
        given:
        Errors errors

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            errors = service.createDocumentType(title, description)
        }

        then:
        errors

        where:
        title | description
        null  | null
        null  | DESCRIPTION
        TITLE | null
        ""    | ""
        ""    | DESCRIPTION
        TITLE | ""
    }

    void "test createDocumentType, twice with same titles fails"() {
        given:
        Errors errors

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            errors = service.createDocumentType(TITLE, DESCRIPTION)
            errors = service.createDocumentType(TITLE, DESCRIPTION)
        }

        then:
        errors
    }

    void "test deleteDocument, no document for documentType succeeds"() {
        given:
        Errors errors
        DocumentType documentType = DomainFactory.createDocumentType()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            errors = service.deleteDocumentType(documentType)
        }

        then:
        !errors
        !DocumentType.all
    }

    void "test deleteDocument, one document for documentType succeeds"() {
        given:
        Errors errors
        Document document = DomainFactory.createDocument()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            errors = service.deleteDocumentType(document.documentType)
        }

        then:
        !errors
        !Document.all
        !DocumentType.all
    }

    void "test deleteDocument, no documentType fails"() {
        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            service.deleteDocumentType(null)
        }

        then:
        thrown(AssertionError)
    }

    void "test updateDocument, not authenticated"() {
        given:
        DocumentType documentType = DomainFactory.createDocumentType()

        when:
        SpringSecurityUtils.doWithAuth(USER) {
            service.updateDocument(documentType, HelperUtils.getUniqueString().bytes, Document.FormatType.PDF)
        }

        then:
        thrown(AccessDeniedException)
    }

    void "test updateDocument, create new document"() {
        given:
        DocumentType documentType = DomainFactory.createDocumentType()
        byte[] content = HelperUtils.getUniqueString().bytes
        Document.FormatType type = Document.FormatType.PDF

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            service.updateDocument(documentType, content, type)
        }

        then:
        Document d = CollectionUtils.exactlyOneElement(Document.all)
        d.documentType == documentType
        d.content == content
        d.formatType == type
    }

    void "test updateDocument, update existing document"() {
        given:
        DocumentType documentType = DomainFactory.createDocumentType()
        DomainFactory.createDocument([
                documentType: documentType,
        ])
        byte[] content = HelperUtils.getUniqueString().bytes
        Document.FormatType type = Document.FormatType.CSV

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            service.updateDocument(documentType, content, type)
        }

        then:
        Document d = CollectionUtils.exactlyOneElement(Document.all)
        d.documentType == documentType
        d.content == content
        d.formatType == type
    }

    void "test listDocumentTypes, not authenticated"() {
        when:
        SpringSecurityUtils.doWithAuth(USER) {
            service.listDocumentTypes()
        }

        then:
        thrown(AccessDeniedException)
    }

    void "test listDocumentTypes and listDocuments, none found"() {
        expect:
        [] == SpringSecurityUtils.doWithAuth(ADMIN) {
            service.listDocumentTypes()
        }
        [] == service.listDocuments()
    }

    void "test listDocumentTypes and listDocuments, documentTypes and documents found"() {
        given:
        Document document = DomainFactory.createDocument()

        expect:
        [document] == service.listDocuments()

        [document.documentType] == SpringSecurityUtils.doWithAuth(ADMIN) {
            service.listDocumentTypes()
        }
    }
}
