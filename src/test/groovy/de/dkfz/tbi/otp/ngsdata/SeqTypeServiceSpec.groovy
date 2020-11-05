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
package de.dkfz.tbi.otp.ngsdata


import grails.testing.gorm.DataTest

class SeqTypeServiceSpec extends MetadataFieldsServiceSpec<SeqType> implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                SeqType,
        ]
    }

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

    void "test changeLegacyState of paired end seq type - should always keep single-end and paired-end in sync"() {
        given:
        SeqType single = DomainFactory.createSeqType()
        SeqType paired = DomainFactory.createSeqTypePaired(
                name: single.name,
                dirName: single.dirName,
                singleCell: single.singleCell, // don't care, just keep same
        )
        boolean legacy = true

        when:
        service.changeLegacyState(single, legacy)

        then:
        single.legacy == legacy
        paired.legacy == legacy
    }

    @Override
    protected MetadataFieldsService getService() {
        return seqTypeService
    }
}
