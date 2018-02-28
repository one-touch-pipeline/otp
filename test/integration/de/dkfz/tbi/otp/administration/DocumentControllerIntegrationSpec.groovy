package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.testing.*
import grails.plugin.springsecurity.*
import org.codehaus.groovy.grails.web.servlet.mvc.*
import org.springframework.security.access.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import static javax.servlet.http.HttpServletResponse.*

class DocumentControllerIntegrationSpec extends Specification implements UserAndRoles {

    DocumentController controller = new DocumentController()

    def setup() {
        createUserAndRoles()
    }


    void "test upload, successful"() {
        given:
        Document.Name name = Document.Name.PROJECT_FORM
        Document.Type type = Document.Type.CSV
        String content = "ABC"

        when:
        controller.request.method = 'POST'
        controller.params.name = name
        controller.params.type = type
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
        controller.flash.message == "The document was stored successfully"
        Document d = exactlyOneElement(Document.all)
        d.name == name
        d.type == type
        d.content == content.bytes
    }

    @Unroll
    void "test upload, fails because of missing #problem"() {
        given:
        Document.Name name = Document.Name.PROJECT_FORM
        Document.Type type = Document.Type.CSV
        String content = "ABC"

        when:
        controller.request.method = 'POST'
        controller.params.name = name
        controller.params.type = type
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
        controller.flash.message == "The document could not be stored"
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
        Document.Name name = Document.Name.PROJECT_FORM
        Document.Type type = Document.Type.CSV
        String content = "ABC"

        DomainFactory.createDocument(
                name: name,
                content: content,
                type: type,
        )

        when:
        controller.params.file = name
        controller.params.to = to

        doWithAnonymousAuth {
            controller.download()
        }

        then:
        controller.response.status == SC_OK
        controller.response.contentType.startsWith(Document.Type.CSV.mimeType)
        controller.response.header("Content-Disposition") == ((to == DocumentController.Action.DOWNLOAD) ?
                "attachment;filename=${name.toString().toLowerCase()}.${type.extension}" :
                null)
        controller.response.contentAsByteArray == content.bytes

        where:
        to                                 | _
        DocumentController.Action.DOWNLOAD | _
        DocumentController.Action.VIEW     | _
    }

    void "test manage"() {
        given:
        Document.Name name = Document.Name.PROJECT_FORM

        Document document = DomainFactory.createDocument(
                name: name,
        )

        when:
        def model = SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.manage()
        }

        then:
        controller.response.status == SC_OK
        model.documents instanceof Map
        model.documents.get(name) == document
        model.documents.keySet() == Document.Name.values() as Set
    }
}
