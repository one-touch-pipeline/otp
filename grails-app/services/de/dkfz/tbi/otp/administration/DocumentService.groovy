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

import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.repository.administration.document.DocumentDataService
import de.dkfz.tbi.otp.repository.administration.document.DocumentTypeDataService

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@Transactional
@GrailsCompileStatic
class DocumentService {
    DocumentDataService documentDataService
    DocumentTypeDataService documentTypeDataService

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<DocumentType> listDocumentTypes() {
        return documentTypeDataService.findAll()
    }

    List<Document> listDocuments() {
        return documentDataService.findAll().sort { a, b ->
            a.documentType.sortOrder <=> b.documentType.sortOrder ?: a.documentType.title <=> b.documentType.title
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Errors createDocumentType(String title, String description) {
        try {
            documentTypeDataService.save(title, description)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Errors deleteDocumentType(DocumentType documentType) {
        assert documentType: "documentType is null"

        Document document = atMostOneElement(documentDataService.findAllByDocumentType(documentType))
        try {
            if (document) {
                documentDataService.delete(document.id)
            }
            documentTypeDataService.delete(documentType.id)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Errors updateDocument(DocumentType documentType, byte[] content, Document.FormatType formatType) {
        Document document = atMostOneElement(documentDataService.findAllByDocumentType(documentType))
        try {
            if (document) {
                documentDataService.updateDocument(document.id, content, formatType)
            } else {
                documentDataService.saveDocument(documentType, content, formatType)
            }
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Errors updateSortOrder(DocumentType documentType, int sortOrder) {
        documentType.sortOrder = sortOrder
        try {
            documentTypeDataService.save(documentType)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Errors updateDescription(DocumentType documentType, String description) {
        documentType.description = description
        try {
            documentTypeDataService.save(documentType)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    static String getDocumentFileNameWithExtension(Document document) {
        return "${document.documentType.title}.${document.formatType.extension}".toLowerCase()
    }
}
