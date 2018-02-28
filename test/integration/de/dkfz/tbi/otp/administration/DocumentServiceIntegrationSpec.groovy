package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.testing.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.*
import grails.test.spock.*
import org.springframework.beans.factory.annotation.*
import org.springframework.security.access.*

class DocumentServiceIntegrationSpec extends IntegrationSpec implements UserAndRoles {

    @Autowired
    DocumentService service


    void "test getDocument, available document"() {
        given:
        Document.Name name = Document.Name.PROJECT_FORM
        Document document = DomainFactory.createDocument(
                name: name,
        )

        expect:
        document == service.getDocument(name)
    }

    void "test getDocument, document not available"() {
        expect:
        null == service.getDocument(Document.Name.PROJECT_FORM)
    }


    void "test updateDocument, not authenticated"() {
        given:
        createUserAndRoles()

        when:
        SpringSecurityUtils.doWithAuth(USER) {
            service.updateDocument(Document.Name.PROJECT_FORM, "adf".getBytes(), Document.Type.PDF)
        }

        then:
        thrown(AccessDeniedException)
    }

    void "test updateDocument, create new document"() {
        given:
        createUserAndRoles()
        Document.Name name = Document.Name.PROJECT_FORM
        byte[] content = "asdf".getBytes()
        Document.Type type = Document.Type.PDF

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            service.updateDocument(name, content, type)
        }

        then:
        Document d = CollectionUtils.exactlyOneElement(Document.all)
        d.name == name
        d.content == content
        d.type == type
    }

    void "test updateDocument, update existing document"() {
        given:
        createUserAndRoles()
        Document.Name name = Document.Name.PROJECT_FORM
        DomainFactory.createDocument(
                name: name,
        )
        byte[] content = "qwertz".getBytes()
        Document.Type type = Document.Type.PDF

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            service.updateDocument(name, content, type)
        }

        then:
        Document d = CollectionUtils.exactlyOneElement(Document.all)
        d.name == name
        d.content == content
        d.type == type
    }


    void "test listDocuments, not authenticated"() {
        given:
        createUserAndRoles()

        when:
        SpringSecurityUtils.doWithAuth(USER) {
            service.listDocuments()
        }

        then:
        thrown(AccessDeniedException)
    }

    void "test listDocuments, none found"() {
        given:
        createUserAndRoles()

        expect:
        [] == SpringSecurityUtils.doWithAuth(ADMIN) {
            service.listDocuments()
        }
    }

    void "test listDocuments, documents found"() {
        given:
        createUserAndRoles()
        Document document = DomainFactory.createDocument(
                name: Document.Name.PROJECT_FORM,
        )

        expect:
        [document] == SpringSecurityUtils.doWithAuth(ADMIN) {
            service.listDocuments()
        }
    }
}
