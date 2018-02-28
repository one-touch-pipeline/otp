package de.dkfz.tbi.otp.administration

import grails.compiler.*
import grails.validation.ValidationException
import org.springframework.security.access.prepost.*
import org.springframework.validation.Errors

@GrailsCompileStatic
class DocumentService {

    Document getDocument(Document.Name name) {
        Document.findByName(name)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Errors updateDocument(Document.Name name, byte[] content, Document.Type type) {
        Document document = Document.findByName(name) ?: new Document(name: name)
        document.content = content
        document.type = type
        try {
            document.save(flush: true)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<Document> listDocuments() {
        Document.all
    }
}
