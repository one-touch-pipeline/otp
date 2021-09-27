/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.repository.administration.document

import grails.test.hibernate.HibernateSpec
import spock.lang.Shared

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.administration.Document
import de.dkfz.tbi.otp.administration.DocumentType
import de.dkfz.tbi.otp.domainFactory.administration.DocumentFactory

class DocumentDataServiceHibernateSpec extends HibernateSpec implements DocumentFactory {

    @Override
    List<Class> getDomainClasses() {
        return [
                Document,
                DocumentType,
        ]
    }

    @Shared
    DocumentDataService documentDataService

    @Override
    void setup() {
        documentDataService = hibernateDatastore.getService(DocumentDataService)
    }

    void "findAll, when called, then all documents should be returned"() {
        given:
        List<Document> document = [
                createDocument(),
                createDocument(),
        ]

        when:
        List<Document> results = documentDataService.findAll()

        then:
        TestCase.assertContainSame(results, document)
    }

    void "findAllByDocumentType, when called for a specific DocumenType, then only documents of that type should be returned"() {
        given:
        DocumentType documentType = createDocumentType()
        List<Document> document = [
                createDocument([documentType: documentType]),
                createDocument([documentType: documentType]),
        ]
        (0..5).each {
            createDocument()
        }

        when:
        List<Document> results = documentDataService.findAllByDocumentType(documentType)

        then:
        TestCase.assertContainSame(results, document)
    }

    void "updateDocument, when called with new content and formatType, then a object should be updated"() {
        given:
        Document document = createDocument()
        byte[] content = "new content".bytes

        when:
        Document result = documentDataService.updateDocument(document.id, content, Document.FormatType.PDF)

        then:
        result.id
        result.content == content
        result.formatType == Document.FormatType.PDF
    }

    void "delete, when DocumentType is deleted, then it does not exist anymore"() {
        given:
        Document document1 = createDocument()
        Document document2 = createDocument()

        when:
        documentDataService.delete(document1.id)

        then:
        Document.list() == [document2]
    }

    void "save, when an unsaved DocumentType is given, then save it in the database"() {
        given:
        Document document = createDocument()

        when:
        Document result = documentDataService.save(document)

        then:
        result.id
        result.documentType == document.documentType
        result.content == document.content
        result.formatType == document.formatType
    }

    void "save, when a changed DocumentType is given, then update it in the database"() {
        given:
        Document document = createDocument()
        DocumentType documentType = createDocumentType()
        byte[] content = "other content".bytes

        when:
        document.documentType = documentType
        document.content = content
        documentDataService.save(document)

        then:
        Document inDatabase = Document.get(document.id)
        inDatabase.documentType == document.documentType
        inDatabase.content == document.content
        inDatabase.formatType == document.formatType
    }
}
