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
import de.dkfz.tbi.otp.administration.DocumentType
import de.dkfz.tbi.otp.domainFactory.administration.DocumentFactory

class DocumentTypeDataServiceHibernateSpec extends HibernateSpec implements DocumentFactory {

    @Override
    List<Class> getDomainClasses() {
        return [
                DocumentType,
        ]
    }

    @Shared
    DocumentTypeDataService documentTypeDataService

    @Override
    void setup() {
        documentTypeDataService = hibernateDatastore.getService(DocumentTypeDataService)
    }

    void "findAll, when called, then all documentTypes should be returned"() {
        given:
        List<DocumentType> documentTypes = [
                createDocumentType(),
                createDocumentType(),
        ]

        when:
        List<DocumentType> results = documentTypeDataService.findAll()

        then:
        TestCase.assertContainSame(results, documentTypes)
    }

    void "save, when given with title and description, then a new object is created"() {
        given:
        String title = "testTitle"
        String description = "blabla"

        when:
        DocumentType result = documentTypeDataService.save(title, description)

        then:
        result.id
        result.title == title
        result.description == description
    }

    void "delete, when DocumentType is deleted, then it does not exist anymore"() {
        given:
        DocumentType d1 = createDocumentType()
        DocumentType d2 = createDocumentType()

        when:
        documentTypeDataService.delete(d1.id)

        then:
        DocumentType.list() == [d2]
    }

    void "save, when an unsaved DocumentType is given, then save it in the database"() {
        given:
        DocumentType d1 = createDocumentType()

        when:
        DocumentType result = documentTypeDataService.save(d1)

        then:
        result.id
        result.title == d1.title
        result.description == d1.description
    }

    void "save, when a changed DocumentType is given, then update it in the database"() {
        given:
        DocumentType d1 = createDocumentType()

        when:
        d1.title = "newTitle"
        d1.description = "newDescription"
        documentTypeDataService.save(d1)

        then:
        DocumentType inDatabase = DocumentType.get(d1.id)
        inDatabase.title == "newTitle"
        inDatabase.description == "newDescription"
    }
}
