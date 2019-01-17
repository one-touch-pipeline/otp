package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.*
import grails.test.spock.*
import org.springframework.beans.factory.annotation.*
import org.springframework.security.access.*
import org.springframework.validation.*

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
