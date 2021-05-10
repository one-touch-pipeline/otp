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
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.utils.CollectionUtils

@Transactional
@GrailsCompileStatic
class DocumentService {

    @CompileDynamic
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<DocumentType> listDocumentTypes() {
        DocumentType.all
    }

    @CompileDynamic
    List<Document> listDocuments() {
        Document.all.sort { a, b ->
            a.documentType.sortOrder <=> b.documentType.sortOrder ?: a.documentType.title <=> b.documentType.title
        }
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

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Errors updateSortOrder(DocumentType documentType, int sortOrder) {
        documentType.sortOrder = sortOrder
        try {
            documentType.save(flush: true)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Errors updateDescription(DocumentType documentType, String description) {
        documentType.description = description
        try {
            documentType.save(flush: true)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }
}
