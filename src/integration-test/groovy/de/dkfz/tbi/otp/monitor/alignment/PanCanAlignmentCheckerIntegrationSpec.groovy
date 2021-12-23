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
package de.dkfz.tbi.otp.monitor.alignment

import grails.gorm.transactions.Rollback

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.monitor.MonitorOutputCollector
import de.dkfz.tbi.otp.ngsdata.*

@Rollback
class PanCanAlignmentCheckerIntegrationSpec extends AbstractAlignmentCheckerIntegrationSpec {

    @Override
    AbstractAlignmentChecker createAlignmentChecker() {
        return new PanCanAlignmentChecker()
    }

    @Override
    Pipeline createPipeLine() {
        return DomainFactory.createPanCanPipeline()
    }

    @Override
    Pipeline createPipeLineForCrosschecking() {
        return DomainFactory.createRoddyRnaPipeline()
    }

    void "workflowName, should return PanCanWorkflow"() {
        expect:
        'PanCanWorkflow' == createAlignmentChecker().workflowName
    }

    void "pipeLineName, should return PANCAN_ALIGNMENT"() {
        expect:
        Pipeline.Name.PANCAN_ALIGNMENT == createAlignmentChecker().pipeLineName
    }

    void "seqTypes, should return WGS and WES"() {
        given:
        List<SeqType> seqTypes = [
                DomainFactory.createWholeGenomeSeqType(),
                DomainFactory.createExomeSeqType(),
                DomainFactory.createChipSeqType(),
        ]

        expect:
        TestCase.assertContainSame(seqTypes, createAlignmentChecker().seqTypes)
    }

    void "filter, when seqTracks given, then create output for filtered seqTracks and return the others"() {
        given:
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        checker = Spy(PanCanAlignmentChecker)

        SeqTrack wgs = DomainFactory.createSeqTrack(seqType: DomainFactory.createWholeGenomeSeqType())

        SeqTrack wesFine = createExomeSeqTrack(true, true)

        SeqTrack wesNoLibraryPreparationKit = createExomeSeqTrack(false, false)

        SeqTrack wesNoBedFile = createExomeSeqTrack(true, false)

        List<SeqTrack> seqTracks = [
                wgs,
                wesFine,
                wesNoLibraryPreparationKit,
                wesNoBedFile,
        ]

        List<SeqTrack> expectedSeqTracks = [
                wgs,
                wesFine,
        ]

        when:
        List<RoddyBamFile> result = checker.filter(seqTracks, output)

        then:
        1 * output.showList(PanCanAlignmentChecker.HEADER_EXOME_WITHOUT_LIBRARY_PREPERATION_KIT, [wesNoLibraryPreparationKit])

        then:
        1 * output.showList(PanCanAlignmentChecker.HEADER_EXOME_NO_BEDFILE, [wesNoBedFile])

        then:
        expectedSeqTracks == result
    }

    private SeqTrack createExomeSeqTrack(boolean createLibraryPreperationKit, boolean createBedFile) {
        SeqTrack exomeSeqTrack = DomainFactory.createExomeSeqTrack([
                libraryPreparationKit: createLibraryPreperationKit ? DomainFactory.createLibraryPreparationKit() : null,
                kitInfoReliability   : createLibraryPreperationKit ? InformationReliability.KNOWN : InformationReliability.UNKNOWN_UNVERIFIED,
        ])
        DomainFactory.createReferenceGenomeProjectSeqType([
                project: exomeSeqTrack.project,
                seqType: exomeSeqTrack.seqType,
        ])
        if (createBedFile) {
            DomainFactory.createBedFile([
                    referenceGenome      : exomeSeqTrack.configuredReferenceGenome,
                    libraryPreparationKit: exomeSeqTrack.libraryPreparationKit,
            ])
        }
        return exomeSeqTrack
    }
}
