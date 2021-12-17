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
import grails.validation.ValidationException
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore

class SeqTrackSpec extends Specification implements DataTest, DomainFactoryCore {

    private final static String FIRST_DATAFILE_NAME = "4_NoIndex_L004_R1_complete_filtered.fastq.gz"
    private final static String SECOND_DATAFILE_NAME = "4_NoIndex_L004_R2_complete_filtered.fastq.gz"
    private final static String COMMON_PREFIX = "4_NoIndex_L004"

    @Override
    Class[] getDomainClassesToMock() {
        [
                DataFile,
                MergingWorkPackage,
                Realm,
                SeqTrack,
        ]
    }

    @Unroll
    void "validate, when has #hasLibPrepKit libraryPreparationKit and kitInfoReliability is #kitInfoReliability, then validation should pass"() {
        given:
        SeqTrack seqTrack = createSeqTrack()
        LibraryPreparationKit libraryPreparationKit = libraryPreparationKitClosure()

        when:
        seqTrack.libraryPreparationKit = libraryPreparationKit
        seqTrack.kitInfoReliability = kitInfoReliability
        seqTrack.validate()

        then:
        !seqTrack.errors.hasErrors()

        where:
        hasLibPrepKit | libraryPreparationKitClosure      | kitInfoReliability
        'no'          | { null }                          | InformationReliability.UNKNOWN_UNVERIFIED
        'no'          | { null }                          | InformationReliability.UNKNOWN_VERIFIED
        ''            | { createLibraryPreparationKit() } | InformationReliability.KNOWN
        ''            | { createLibraryPreparationKit() } | InformationReliability.INFERRED
    }

    @Unroll
    void "validate, when has #hasLibPrepKit libraryPreparationKit and kitInfoReliability is #kitInfoReliability, then validation should fail"() {
        given:
        SeqTrack seqTrack = createSeqTrack()
        LibraryPreparationKit libraryPreparationKit = libraryPreparationKitClosure()

        when:
        seqTrack.libraryPreparationKit = libraryPreparationKit
        seqTrack.kitInfoReliability = kitInfoReliability

        then:
        TestCase.assertValidateError(seqTrack, 'libraryPreparationKit', 'validator.invalid', seqTrack.libraryPreparationKit)

        where:
        hasLibPrepKit | libraryPreparationKitClosure      | kitInfoReliability
        ''            | { createLibraryPreparationKit() } | InformationReliability.UNKNOWN_UNVERIFIED
        ''            | { createLibraryPreparationKit() } | InformationReliability.UNKNOWN_VERIFIED
        'no'          | { null }                          | InformationReliability.KNOWN
        'no'          | { null }                          | InformationReliability.INFERRED
    }

    @Unroll
    void "validate, SeqTrack has to be unique by laneId, run, singleCellWellLabel and project"() {
        given:
        Map properties = [
                laneId      : "lane_id",
                run         : createRun(),
                singleCellWellLabel: "singleCellWellLabel",
                sample      : createSample(),
        ]
        properties.remove(property)

        SeqTrack seqTrackA = createSeqTrack([singleCellWellLabel: "CP_A"] + properties)
        SeqTrack seqTrackB = createSeqTrack([singleCellWellLabel: "CP_B"] + properties)
        seqTrackB[property] = seqTrackA[property]

        when:
        seqTrackB.save(flush: true)

        then:
        thrown(ValidationException)

        where:
        property << ["laneId", "run", "singleCellWellLabel", "sample"]
    }

    @Unroll
    void "normalizeLibraryName, when input is '#value', then result should be '#result"() {
        expect:
        result == SeqTrack.normalizeLibraryName(value)

        where:
        value        || result
        null         || null
        ''           || ''
        '1'          || '1'
        'lib_1'      || '1'
        'lib-1'      || '1'
        'lib000'     || '0'
        'lib0001'    || '1'
        'library1'   || '1'
        'library001' || '1'
    }

    @Unroll
    void "getLongestCommonPrefixBeforeLastUnderscore, when input is '#value1' and '#value2', then common prefix should '#result"() {
        expect:
        result == SeqTrack.getLongestCommonPrefixBeforeLastUnderscore(value1, value2)

        where:
        value1                  | value2                  || result
        FIRST_DATAFILE_NAME     | FIRST_DATAFILE_NAME     || '4_NoIndex_L004_R1_complete'
        FIRST_DATAFILE_NAME     | SECOND_DATAFILE_NAME    || COMMON_PREFIX
        'NoUnderScoreR1.tar.gz' | 'NoUnderScoreR2.tar.gz' || 'NoUnderScoreR'
    }

    @Unroll
    void "getLongestCommonPrefixBeforeLastUnderscore, when input is '#value1' and '#value2', then method should fail"() {
        when:
        SeqTrack.getLongestCommonPrefixBeforeLastUnderscore(value1, value2)

        then:
        thrown(AssertionError)

        where:
        value1              | value2               || result
        null                | SECOND_DATAFILE_NAME || ''
        FIRST_DATAFILE_NAME | null                 || ''
        ''                  | SECOND_DATAFILE_NAME || ''
        FIRST_DATAFILE_NAME | ''                   || ''
    }

    void "getReadGroupName, when SeqTrack has only one dataFile, then throw AssertionError"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithDataFiles(DomainFactory.createMergingWorkPackage())
        DataFile.findAllBySeqTrack(seqTrack)[0].delete(flush: true)

        when:
        seqTrack.getReadGroupName()

        then:
        thrown(AssertionError)
    }

    void "getReadGroupName, when SeqTrack has more then two files, then throw AssertionError"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithDataFiles(DomainFactory.createMergingWorkPackage())
        DomainFactory.createSequenceDataFile([seqTrack: seqTrack])

        when:
        seqTrack.getReadGroupName()

        then:
        thrown(AssertionError)
    }

    void "getReadGroupName, when sequencing read type is paired, then return name consist of: 'run', runname, common file name till underscore"() {
        given:
        MergingWorkPackage mwp = DomainFactory.createMergingWorkPackage(seqType: DomainFactory.createRnaPairedSeqType())
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithDataFiles(mwp)

        when:
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)
        dataFiles[0].vbpFileName = '4_NoIndex_L004_R1_complete_filtered.fastq.gz'
        dataFiles[1].vbpFileName = '4_NoIndex_L004_R2_complete_filtered.fastq.gz'

        then:
        "run${seqTrack.run.name}_4_NoIndex_L004" == seqTrack.getReadGroupName()
    }

    void "getReadGroupName, when sequencing read type is single, then return file name consist of: 'run', runname, file name till first dot"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithOneDataFile([:], [vbpFileName: "fileName.fastq.gz"])

        expect:
        "run${seqTrack.run.name}_${'fileName'}" == seqTrack.getReadGroupName()
    }

    void "test getNReads, returns null"() {
        given:
        SeqTrack seqTrack = createSeqTrack()
        createDataFile([seqTrack: seqTrack, nReads: null])
        createDataFile([seqTrack: seqTrack, nReads: input])

        expect:
        seqTrack.getNReads() == null

        where:
        input | _
        null  | _
        420   | _
    }

    void "test getNReads, returns sum"() {
        given:
        SeqTrack seqTrack = createSeqTrack()
        createDataFile([seqTrack: seqTrack, nReads: 525])
        createDataFile([seqTrack: seqTrack, nReads: 25])

        expect:
        seqTrack.getNReads() == 550
    }

    void "totalFileSize, returns total fileSize of all DataFiles"() {
        given:
        SeqTrack seqTrack = createSeqTrack()
        createDataFile([seqTrack: seqTrack, fileSize: 100])
        createDataFile([seqTrack: seqTrack, fileSize: 200])

        expect:
        seqTrack.totalFileSize() == 300
    }

    void "totalFileSize, when there are not dataFiles return 0"() {
        given:
        SeqTrack seqTrack = createSeqTrack()

        expect:
        seqTrack.totalFileSize() == 0
    }
}
