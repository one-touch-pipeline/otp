package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.testing.*
import de.dkfz.tbi.otp.utils.CollectionUtils
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
