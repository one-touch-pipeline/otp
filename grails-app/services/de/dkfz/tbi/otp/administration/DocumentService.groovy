package de.dkfz.tbi.otp.administration

import grails.compiler.*
import grails.validation.ValidationException
import org.springframework.security.access.prepost.*
import org.springframework.validation.Errors

@GrailsCompileStatic
class DocumentService {

    Document getDocument(Document.Name t) {
        Document.findByName(t)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Errors updateDocument(Document.Name t, byte[] content, Document.Type type) {
        Document document = Document.findByName(t) ?: new Document(name: t)
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
