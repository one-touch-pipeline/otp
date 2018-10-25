package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.utils.CollectionUtils
import grails.compiler.*
import grails.validation.ValidationException
import org.springframework.security.access.prepost.*
import org.springframework.validation.Errors

@GrailsCompileStatic
class DocumentService {

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<DocumentType> listDocumentTypes() {
        DocumentType.all
    }

    List<Document> listDocuments() {
        Document.all
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Errors createDocumentType(String title, String description) {
        DocumentType documentType = new DocumentType(title: title, description: description)
        try {
            documentType.save(flush: true)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Errors deleteDocumentType(DocumentType documentType) {
        assert documentType: "documentType is null"

        Document document = CollectionUtils.atMostOneElement(Document.findAllByDocumentType(documentType))
        try {
            if (document) {
                document.delete(flush: true)
            }
            documentType.delete(flush: true)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Errors updateDocument(DocumentType documentType, byte[] content, Document.FormatType formatType) {
        Document document = Document.findByDocumentType(documentType) ?: new Document(documentType: documentType)
        document.content = content
        document.formatType = formatType
        try {
            document.save(flush: true)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }
}
