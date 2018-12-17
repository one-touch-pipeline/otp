package de.dkfz.tbi.otp.ngsdata

import spock.lang.Specification

abstract class MetadataFieldsServiceSpec<T> extends Specification {

    abstract protected MetadataFieldsService getService()
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
        null == service.findByNameOrImportAlias("unknown")
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
        t == service.findByNameOrImportAlias(NAME,properties)
        t == service.findByNameOrImportAlias(IMPORT_ALIAS,properties)
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
        service.<T> create(name,properties)

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

    Map<String, Long> createObjectsForImportAliasTests() {
        return [
            DomainFactory.<T> createDomainWithImportAlias(service.clazz, [name: NAME] + properties),
            DomainFactory.<T> createDomainWithImportAlias(service.clazz, [name: OTHER_NAME, importAlias: [IMPORT_ALIAS]] + otherProperties)
        ].collectEntries {
            [(it.name as String): it.id as Long]
        }
    }
}
