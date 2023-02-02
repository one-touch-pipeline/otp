/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.qaalignmentoverview

import grails.test.hibernate.HibernateSpec
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.ngsdata.*

class QaOverviewFetchDataServiceHibernateSpec extends HibernateSpec implements RoddyPancanFactory {

    private QaOverviewFetchDataService service

    @Override
    List<Class> getDomainClasses() {
        return [
                FastqImportInstance,
                MergingWorkPackage,
                RoddyBamFile,
        ]
    }

    private void setupData() {
        service = new QaOverviewFetchDataService()
    }

    @Unroll
    void "addLibraryPreparationKitAndSequencingLengthInformation, when LibraryPreparationKit is #kitNames, then name is '#libraryPreparationKitName' and shortname' is '#libraryPreparationKitShortName'"() {
        given:
        setupData()

        List<LibraryPreparationKit> kits = kitNames.collect {
            if (it == null) {
                return null
            }
            findOrCreateLibraryPreparationKit([
                    name            : "kit_${it}".toString(),
                    shortDisplayName: "short_${it}".toString(),
            ])
        }

        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage()
        kits.each {
            createSeqTrackForQa(mergingWorkPackage, it)
        }

        Map<String, ?> map = [
                mergingWorkPackageId: mergingWorkPackage.id,
        ]

        Map<String, ?> expectedMap = map + [
                libraryPreparationKitName     : libraryPreparationKitName,
                libraryPreparationKitShortName: libraryPreparationKitShortName,
                readLength                    : 100.0,
        ]

        when:
        service.addLibraryPreparationKitAndSequencingLengthInformation([map])

        then:
        TestCase.assertContainSame(map, expectedMap)

        where:
        kitNames  || libraryPreparationKitName | libraryPreparationKitShortName
        [1]       || 'kit_1'                   | 'short_1'
        [1, 1]    || 'kit_1'                   | 'short_1'
        [1, 2]    || 'kit_1, kit_2'            | 'short_1, short_2'
        [1, 2, 3] || 'kit_1, kit_2, kit_3'     | 'short_1, short_2, short_3'
        [3, 2, 1] || 'kit_1, kit_2, kit_3'     | 'short_1, short_2, short_3'
        [1, 2, 1] || 'kit_1, kit_2'            | 'short_1, short_2'
        [null]    || '-'                       | '-'
        [1, null] || 'kit_1, -'                | 'short_1, -'
    }

    @Unroll
    void "addLibraryPreparationKitAndSequencingLengthInformation, when sequenceLength is #sequenceLength, then result is #readLength"() {
        given:
        setupData()

        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage()
        createSeqTrackForQa(mergingWorkPackage, null, sequenceLength)

        Map<String, ?> map = [
                mergingWorkPackageId: mergingWorkPackage.id,
        ]

        Map<String, ?> expectedMap = map + [
                libraryPreparationKitName     : '-',
                libraryPreparationKitShortName: '-',
                readLength                    : readLength,
        ]

        when:
        service.addLibraryPreparationKitAndSequencingLengthInformation([map])

        then:
        TestCase.assertContainSame(map, expectedMap)

        where:
        sequenceLength || readLength
        '100'          || 100.0
        '200'          || 200.0
        '100-200'      || 150.0
        '2-3'          || 2.5
        null           || null
    }

    private SeqTrack createSeqTrackForQa(MergingWorkPackage mergingWorkPackage, LibraryPreparationKit libraryPreparationKit, String sequenceLength = "100") {
        SeqTrack seqTrack = createSeqTrackWithTwoDataFile([
                sample               : mergingWorkPackage.sample,
                seqType              : mergingWorkPackage.seqType,
                libraryPreparationKit: libraryPreparationKit
        ], [
                sequenceLength: sequenceLength
        ], [
                sequenceLength: sequenceLength
        ])
        mergingWorkPackage.addToSeqTracks(seqTrack)
        mergingWorkPackage.save(flush: true)
        return seqTrack
    }
}
