package de.dkfz.tbi.otp.ngsdata

import spock.lang.*

@Mock([
        SeqType,
])
class SeqTypeServiceSpec extends MetadataFieldsServiceSpec<SeqType> {

    SeqTypeService seqTypeService = new SeqTypeService()

    final static String SEQ_TYPE_DIR = "SeqTypeDir"

    final static String SEQ_TYPE_DISPLAY_NAME = "SeqTypeDisplayName"

    def setup() {
        properties = [
                dirName      : SEQ_TYPE_DIR,
                displayName  : SEQ_TYPE_DISPLAY_NAME,
                libraryLayout: LibraryLayout.SINGLE,
                singleCell   : false,
        ]
        otherProperties = [
                dirName      : "Other${SEQ_TYPE_DIR}",
                displayName  : "Other${SEQ_TYPE_DISPLAY_NAME}",
                libraryLayout: LibraryLayout.SINGLE,
                singleCell   : false,
        ]
    }

    void "test createSeqType with invalid displayName, fails"() {
        given:
        DomainFactory.<SeqType> createDomainWithImportAlias(service.clazz, [name: NAME, importAlias: [IMPORT_ALIAS]] + properties)

        when:
        service.create("newName", [displayName: displayName, libraryLayout: LibraryLayout.SINGLE, singleCell: false])

        then:
        thrown(AssertionError)

        where:
        displayName           | _
        NAME                  | _
        IMPORT_ALIAS          | _
        SEQ_TYPE_DISPLAY_NAME | _


    }

    void "test createSeqType fails with null as argument"() {
        when:
        service.create(name,
                [
                        dirName      : dirName,
                        displayName  : SEQ_TYPE_DISPLAY_NAME,
                        libraryLayout: libraryLayout,
                        singleCell   : singleCell,
                ]
        )

        then:
        thrown(AssertionError)

        where:
        name | dirName      | libraryLayout                | singleCell
        null | SEQ_TYPE_DIR | LibraryLayout.SINGLE | false
        NAME | null         | LibraryLayout.SINGLE | false
        NAME | SEQ_TYPE_DIR | null                         | false
        NAME | SEQ_TYPE_DIR | LibraryLayout.SINGLE | null
    }

    void "test createMultiple fails with invalid arguments"() {
        when:
        service.createMultiple(name, libraryLayouts,
                [
                        dirName      : dirName,
                        displayName  : SEQ_TYPE_DISPLAY_NAME,
                        libraryLayout: libraryLayouts,
                        singleCell   : singleCell,
                ]
        )
        then:
        thrown(AssertionError)

        where:
        name | dirName      | libraryLayouts                 | singleCell
        null | SEQ_TYPE_DIR | [LibraryLayout.SINGLE] | false
        NAME | null         | [LibraryLayout.SINGLE] | false
        NAME | SEQ_TYPE_DIR | null                           | false
        NAME | SEQ_TYPE_DIR | []                             | false
        NAME | SEQ_TYPE_DIR | ["invalid"]                    | false
        NAME | SEQ_TYPE_DIR | [LibraryLayout.SINGLE] | null
    }

    void "test create SeqType with and without single cell, succeeds"() {
        when:
        service.create(NAME, [libraryLayout: LibraryLayout.PAIRED, dirName: SEQ_TYPE_DIR, displayName: SEQ_TYPE_DISPLAY_NAME, singleCell: false])
        service.create(NAME, [libraryLayout: LibraryLayout.PAIRED, dirName: "Other${SEQ_TYPE_DIR}", displayName: SEQ_TYPE_DISPLAY_NAME, singleCell: true])

        then:
        service.findByNameOrImportAlias(NAME, [dirName: SEQ_TYPE_DIR, displayName: SEQ_TYPE_DISPLAY_NAME, libraryLayout: LibraryLayout.PAIRED, singleCell: false])
        service.findByNameOrImportAlias(NAME, [dirName: "Other${SEQ_TYPE_DIR}", displayName: SEQ_TYPE_DISPLAY_NAME, libraryLayout: LibraryLayout.PAIRED, singleCell: true])
    }

    void "test createMultiple with name, succeeds"() {
        when:
        service.createMultiple(NAME, [LibraryLayout.PAIRED, LibraryLayout.SINGLE], [dirName: SEQ_TYPE_DIR, displayName: SEQ_TYPE_DISPLAY_NAME, singleCell: false])
        service.createMultiple(NAME, [LibraryLayout.MATE_PAIR], [dirName: "Other${SEQ_TYPE_DIR}", displayName: SEQ_TYPE_DISPLAY_NAME, singleCell: true])

        then:
        service.findByNameOrImportAlias(NAME, [dirName: SEQ_TYPE_DIR, displayName: SEQ_TYPE_DISPLAY_NAME, libraryLayout: LibraryLayout.SINGLE, singleCell: false])
        service.findByNameOrImportAlias(NAME, [dirName: SEQ_TYPE_DIR, displayName: SEQ_TYPE_DISPLAY_NAME, libraryLayout: LibraryLayout.PAIRED, singleCell: false])
        !service.findByNameOrImportAlias(NAME, [dirName: SEQ_TYPE_DIR, displayName: SEQ_TYPE_DISPLAY_NAME, libraryLayout: LibraryLayout.MATE_PAIR, singleCell: false])
        !service.findByNameOrImportAlias(NAME, [dirName: "Other${SEQ_TYPE_DIR}", displayName: SEQ_TYPE_DISPLAY_NAME, libraryLayout: LibraryLayout.SINGLE, singleCell: true])
        !service.findByNameOrImportAlias(NAME, [dirName: "Other${SEQ_TYPE_DIR}", displayName: SEQ_TYPE_DISPLAY_NAME, libraryLayout: LibraryLayout.PAIRED, singleCell: true])
        service.findByNameOrImportAlias(NAME, [dirName: "Other${SEQ_TYPE_DIR}", displayName: SEQ_TYPE_DISPLAY_NAME, libraryLayout: LibraryLayout.MATE_PAIR, singleCell: true])
    }

    @Override
    protected MetadataFieldsService getService() {
        return seqTypeService
    }
}
