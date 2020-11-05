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
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.utils.CollectionUtils

class SeqTypeSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                SeqType,
        ]
    }

    void "test get SingleCell SeqTypes, no SeqType in DB, returns empty list"() {
        expect:
        [] == SeqTypeService.allSingleCellSeqTypes
    }

    void "test get SingleCell SeqTypes, returns all seqTypes with singleCell true"() {
        when:
        List<SeqType> expectedSeqTypes = [
                createSeqType(singleCell: true),
                createSeqType(singleCell: true),
        ]
        createSeqType(singleCell: false)

        then:
        CollectionUtils.containSame(expectedSeqTypes, SeqTypeService.allSingleCellSeqTypes)
    }

    void "test get WGS Paired SeqType, no SeqType in DB, should fail"() {
        when:
        SeqTypeService.wholeGenomePairedSeqType

        then:
        AssertionError e = thrown()
        e.message.contains("WGS ${LibraryLayout.PAIRED} not found")
    }

    void "test get WGS Paired SeqType, no WGS Paired SeqType in DB, should fail"() {
        when:
        DomainFactory.createWholeGenomeSeqType(LibraryLayout.SINGLE)
        SeqTypeService.wholeGenomePairedSeqType

        then:
        AssertionError e = thrown()
        e.message.contains("WGS ${LibraryLayout.PAIRED} not found")
    }

    void "test get WGS Paired SeqType, All Fine"() {
        when:
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        then:
        seqType == SeqTypeService.wholeGenomePairedSeqType
    }


    void "test get Exome Paired SeqType, no SeqType in DB, should fail"() {
        when:
        SeqTypeService.exomePairedSeqType

        then:
        AssertionError e = thrown()
        e.message.contains("WES ${LibraryLayout.PAIRED} not found")
    }

    void "test get Exome Paired SeqType, no Exome Paired SeqType in DB, should fail"() {
        when:
        DomainFactory.createExomeSeqType(LibraryLayout.SINGLE)
        SeqTypeService.exomePairedSeqType

        then:
        AssertionError e = thrown()
        e.message.contains("WES ${LibraryLayout.PAIRED} not found")
    }

    void "test get Exome Paired SeqType, AllFine"() {
        when:
        SeqType seqType = DomainFactory.createExomeSeqType()

        then:
        seqType == SeqTypeService.exomePairedSeqType
    }

    void "test create SeqTypes with unique Name And LibraryLayout combination, all fine"() {
        when:
        DomainFactory.createSeqType(
                name         : "seqTypeName1",
                libraryLayout: LibraryLayout.PAIRED,
                dirName      : "name1",
        )
        DomainFactory.createSeqType(
                name         : "seqTypeName1",
                libraryLayout: LibraryLayout.SINGLE,
                dirName      : "name1",
        )
        DomainFactory.createSeqType(
                name: "seqTypeName2",
                libraryLayout: LibraryLayout.PAIRED,
        )

        then:
        SeqType.findAll().size() == 3
    }

    void "test create SeqTypes with non unique Name, dirName and LibraryLayout combination, should fail"() {
        when:
        DomainFactory.createSeqType(
                name         : "seqTypeName1",
                libraryLayout: LibraryLayout.PAIRED,
                dirName      : "name1",
        )
        SeqType seqType = DomainFactory.createSeqType([
                name         : "seqTypeName1",
                libraryLayout: LibraryLayout.PAIRED,
                dirName      : "name1",
        ], false)

        then:
        TestCase.assertAtLeastExpectedValidateError(seqType, "name", "unique", "seqTypeName1")
        TestCase.assertAtLeastExpectedValidateError(seqType, "dirName", "unique", "name1")
    }

    void "test create SeqTypes with non unique Name and different dirName and LibraryLayout combination, should fail"() {
        when:
        DomainFactory.createSeqType(
                name         : "seqTypeName1",
                libraryLayout: LibraryLayout.PAIRED,
                dirName      : "name1",
        )
        SeqType seqType = DomainFactory.createSeqType([
                name         : "seqTypeName1",
                libraryLayout: LibraryLayout.SINGLE,
                dirName      : "name2",
        ], false)

        then:
        TestCase.assertValidateError(seqType, "dirName", "same", "name2")
    }

    void "test create SeqTypes with unique Name, singleCell and LibraryLayout but same dirName combination, should fail"() {
        when:
        DomainFactory.createSeqType(
                name         : "seqTypeName1",
                libraryLayout: LibraryLayout.PAIRED,
                singleCell   : true,
                dirName      : "name1",
        )
        SeqType seqType = DomainFactory.createSeqType([
                name         : "seqTypeName2",
                libraryLayout: LibraryLayout.SINGLE,
                singleCell   : false,
                dirName      : "name1",
        ], false)

        then:
        TestCase.assertValidateError(seqType, "dirName", "unique", "name1")
    }

    void "test create SeqType with non valid dirName, should fail"() {
        when:
        SeqType seqType = DomainFactory.createSeqType([
                name         : "seqTypeName1",
                libraryLayout: LibraryLayout.PAIRED,
                dirName      : "name1\\",
        ], false)

        then:
        TestCase.assertValidateError(seqType, "dirName", "validator.path.component", "name1\\")
    }

    void "ensure only the basic Exome+WholeGenome seqtypes are the 'default' since this assumption is implicit in much old OTP code"() {
        given:
        DomainFactory.createDefaultOtpAlignableSeqTypes()
        List<SeqType> alignableSeqTypes = SeqTypeService.getDefaultOtpAlignableSeqTypes()

        expect:
        2 == alignableSeqTypes.size()
        alignableSeqTypes.find {
            it.name == SeqTypeNames.EXOME.seqTypeName && it.libraryLayout == LibraryLayout.PAIRED && !it.singleCell
        }
        alignableSeqTypes.find {
            it.name == SeqTypeNames.WHOLE_GENOME.seqTypeName && it.libraryLayout == LibraryLayout.PAIRED && !it.singleCell
        }
    }
}
