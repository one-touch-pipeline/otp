/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata

import spock.lang.Specification

abstract class AbstractMetadataFieldsServiceSpec<T> extends Specification {

    abstract protected AbstractMetadataFieldsService getService()
    Map properties = [:]
    Map otherProperties = [:]
    protected final static String NAME = "metadata_name"
    protected final static String OTHER_NAME = "other_metadata_name"
    protected final static String IMPORT_ALIAS = "metadata_importAlias"

    void "test findByNameOrImportAlias with name, succeeds"() {
        when:
        T t = DomainFactory.<T> createDomainWithImportAlias(service.clazz, [name: NAME] + properties)

        then:
        t == service.findByNameOrImportAlias(NAME, properties)
    }

    void "test findByNameOrImportAlias with ImportAlias, succeeds"() {
        when:
        T t = DomainFactory.<T> createDomainWithImportAlias(service.clazz, [name: NAME, importAlias: [IMPORT_ALIAS]] + properties)

        then:
        t == service.findByNameOrImportAlias(IMPORT_ALIAS, properties)
    }

    void "test findByNameOrImportAlias with not registered name, fails"() {
        when:
        DomainFactory.<T> createDomainWithImportAlias(service.clazz, [name: NAME, importAlias: [IMPORT_ALIAS]] + properties)

        then:
        service.findByNameOrImportAlias("unknown") == null
    }

    void  "test findByNameOrImportAlias with name.toUpperCase(), succeeds"() {
        when:
        T t = DomainFactory.<AntibodyTarget>createDomainWithImportAlias(service.clazz, [name: NAME] + properties)
        then:
        t == service.findByNameOrImportAlias(NAME.toUpperCase(), properties)
    }

    void  "test findByNameOrImportAlias with importAlias.toUpperCase(), succeeds"() {
        when:
        T t = DomainFactory.<T>createDomainWithImportAlias(service.clazz, [name: NAME, importAlias: [IMPORT_ALIAS]] + properties)
        then:
        t == service.findByNameOrImportAlias(IMPORT_ALIAS.toUpperCase(), properties)
    }

    void "test create with name, succeeds"() {
        when:
        T t = service.<T> create(NAME, properties)

        then:
        t == service.findByNameOrImportAlias(NAME, properties)
    }

    void "test create with name and importAlias, succeeds"() {
        when:
        T t = service.<T> create(NAME, properties, [IMPORT_ALIAS])

        then:
        t == service.findByNameOrImportAlias(NAME, properties)
        t == service.findByNameOrImportAlias(IMPORT_ALIAS, properties)
    }

    void "test create with name twice, fails"() {
        when:
        service.<T> create(NAME, properties)
        service.<T> create(NAME, properties)

        then:
        thrown(AssertionError)
    }

    void "test create with importAlias twice, fails"() {
        when:
        service.<T> create(NAME, properties, [IMPORT_ALIAS])
        service.<T> create(OTHER_NAME, properties, [IMPORT_ALIAS])

        then:
        thrown(AssertionError)
    }

    void "test create with invalid values for name and importAlias, fails"() {
        when:
        service.<T> create(name, properties)

        then:
        thrown(AssertionError)

        where:
        name | _
        null | _
        ""   | _
    }

    void "test addNewAlias with ID and importAlias, succeeds"() {
        when:
        T t = DomainFactory.<T> createDomainWithImportAlias(service.clazz, [name: NAME] + properties)
        service.addNewAlias(t.id as Long, IMPORT_ALIAS)

        then:
        t.importAlias.contains(IMPORT_ALIAS)
    }

    void "test addNewAlias when ID is null, fails"() {
        when:
        createObjectsForImportAliasTests()
        service.addNewAlias(null, IMPORT_ALIAS)

        then:
        thrown(AssertionError)
    }

    void "test addNewAlias when ID does not exist, fails"() {
        when:
        service.addNewAlias(0, IMPORT_ALIAS)

        then:
        thrown(AssertionError)
    }

    void "test addNewAlias when importAlias is null, fails"() {
        when:
        Map<String, Long> ids = createObjectsForImportAliasTests()
        service.addNewAlias(ids[NAME], null)

        then:
        thrown(AssertionError)
    }

    void "test addNewAlias when name already exists, fails"() {
        when:
        Map<String, Long> ids = createObjectsForImportAliasTests()
        service.addNewAlias(ids[NAME], OTHER_NAME)

        then:
        thrown(AssertionError)
    }

    void "test addNewAlias when name in same T already exists, fails"() {
        when:
        Map<String, Long> ids = createObjectsForImportAliasTests()
        service.addNewAlias(ids[NAME], NAME)

        then:
        thrown(AssertionError)
    }

    void "test addNewAlias when importAlias already exists, fails"() {
        when:
        Map<String, Long> ids = createObjectsForImportAliasTests()
        service.addNewAlias(ids[NAME], IMPORT_ALIAS)

        then:
        thrown(AssertionError)
    }

    void "test addNewAlias when importAlias in same T already exists, fails"() {
        when:
        Map<String, Long> ids = createObjectsForImportAliasTests()
        service.addNewAlias(ids[OTHER_NAME], IMPORT_ALIAS)

        then:
        thrown(AssertionError)
    }

    void "test changeLegacyState"() {
        given:
        T t = DomainFactory.<T> createDomainWithImportAlias(service.clazz, [name: NAME] + properties)

        when:
        service.changeLegacyState(t, legacy)

        then:
        t.legacy == legacy

        where:
        legacy | _
        true   | _
        false  | _
    }

    Map<String, Long> createObjectsForImportAliasTests() {
        return [
            DomainFactory.<T> createDomainWithImportAlias(service.clazz, [name: NAME] + properties),
            DomainFactory.<T> createDomainWithImportAlias(service.clazz, [name: OTHER_NAME, importAlias: [IMPORT_ALIAS]] + otherProperties),
        ].collectEntries {
            [(it.name as String): it.id as Long]
        }
    }
}
