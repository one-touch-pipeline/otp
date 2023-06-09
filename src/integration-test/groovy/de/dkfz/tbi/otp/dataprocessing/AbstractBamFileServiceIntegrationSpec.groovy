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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
class AbstractBamFileServiceIntegrationSpec extends Specification implements RoddyPancanFactory, IsRoddy {

    AbstractBamFileService abstractBamFileService

    void "test calculateCoverageWithN, when needsBedFile is true, return null"() {
        given:
        SeqType seqType = DomainFactory.createExomeSeqType()
        SeqTrack seqTrack = createSeqTrack(seqType: seqType)

        AbstractBamFile abstractBamFile = createBamFile(seqTracks: [seqTrack] as Set)
        abstractBamFileService = new AbstractBamFileService()

        expect:
        abstractBamFileService.calculateCoverageWithN(abstractBamFile) == null
    }

    void "test calculateCoverageWithN, when needsBedFile is false, return result"() {
        given:
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        SeqTrack seqTrack = createSeqTrack(seqType: seqType)

        AbstractBamFile abstractBamFile = createBamFile(seqTracks: [seqTrack] as Set)
        abstractBamFile.workPackage.referenceGenome.length = 10
        abstractBamFile.workPackage.referenceGenome.save(flush: true)
        DomainFactory.createRoddyMergedBamQa(abstractBamFile, [chromosome: RoddyQualityAssessment.ALL, qcBasesMapped: 2])
        abstractBamFileService = new AbstractBamFileService()

        expect:
        abstractBamFileService.calculateCoverageWithN(abstractBamFile) == 0.2
    }

    static final List<String> PROCESSING_STEPS = [
            "aceseq",
            "indel",
            "snv",
            "sophia",
            "runYapsa",
    ].asImmutable()

    def 'checkThatProcessingStepListIsComplete'() {
        given:
        List<Pipeline> analysisPipelines = Pipeline.Name.values().findAll {
            it.type != Pipeline.Type.ALIGNMENT && it != Pipeline.Name.OTP_SNV
        }

        expect:
        analysisPipelines.size() == PROCESSING_STEPS.size()
    }

    void "set SamplePairStatus to need processing while input is null"() {
        when:
        abstractBamFileService.updateSamplePairStatusToNeedProcessing(null)

        then:
        AssertionError error = thrown()
        error.message.contains("The input bam file must not be null")
    }

    @Unroll
    void "set #analysisName samplePairStatus to need processing while samplePair is in state needs processing"() {
        given:
        SamplePair samplePair = setSamplePairStatusToNeedProcessing_setup(SamplePair.ProcessingStatus.NEEDS_PROCESSING, analysisName)
        AbstractBamFile bamFile = CollectionUtils.atMostOneElement(AbstractBamFile.findAllByWorkPackage(samplePair.mergingWorkPackage2))

        when:
        abstractBamFileService.updateSamplePairStatusToNeedProcessing(bamFile)

        then:
        samplePair."${analysisName}ProcessingStatus" == SamplePair.ProcessingStatus.NEEDS_PROCESSING

        where:
        analysisName << PROCESSING_STEPS
    }

    @Unroll
    void "set #analysisName samplePairStatus to need processing while samplePair is in state no processing needed"() {
        given:
        DomainFactory.createAllAnalysableSeqTypes()
        SamplePair samplePair = setSamplePairStatusToNeedProcessing_setup(SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED, analysisName)
        AbstractBamFile bamFile = CollectionUtils.atMostOneElement(AbstractBamFile.findAllByWorkPackage(samplePair.mergingWorkPackage2))

        when:
        abstractBamFileService.updateSamplePairStatusToNeedProcessing(bamFile)

        then:
        samplePair."${analysisName}ProcessingStatus" == SamplePair.ProcessingStatus.NEEDS_PROCESSING

        where:
        analysisName << PROCESSING_STEPS
    }

    @Unroll
    void "set #analysisName samplePair status to need processing while other samplePair in state no processing needed"() {
        given:
        SamplePair samplePair1 = setSamplePairStatusToNeedProcessing_setup(SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED, analysisName)
        SamplePair samplePair2 = setSamplePairStatusToNeedProcessing_setup(SamplePair.ProcessingStatus.NEEDS_PROCESSING, analysisName)
        AbstractBamFile bamFile = CollectionUtils.atMostOneElement(AbstractBamFile.findAllByWorkPackage(samplePair2.mergingWorkPackage2))

        when:
        abstractBamFileService.updateSamplePairStatusToNeedProcessing(bamFile)

        then:
        samplePair1."${analysisName}ProcessingStatus" == SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED

        where:
        analysisName << PROCESSING_STEPS
    }

    void "getActiveBlockedBamsContainingSeqTracks, only returns not withdrawn blocked bams"() {
        given:
        Closure<RoddyBamFile> createRoddyBamFileHelper = { boolean blocked, boolean withdrawn ->
            Map props = [:]
            if (blocked) {
                props << [qcTrafficLightStatus: AbstractBamFile.QcTrafficLightStatus.BLOCKED]
            }
            if (withdrawn) {
                props << [withdrawn: true]
            }
            return createRoddyBamFile(props, RoddyBamFile)
        }
        createRoddyBamFile(RoddyBamFile)
        List<SeqTrack> seqTracks =  [
                createRoddyBamFileHelper(false, false),
                createRoddyBamFileHelper(false, true),
                createRoddyBamFileHelper(true, false),
                createRoddyBamFileHelper(true, true),
        ].collectMany {
            it.containedSeqTracks
        }

        when:
        List<AbstractBamFile> bams = abstractBamFileService.getActiveBlockedBamsContainingSeqTracks(seqTracks)

        then:
        bams.size() == 1
    }

    void "getActiveBlockedBamsContainingSeqTracks, empty merging work package list is properly handled"() {
        expect:
        [] == abstractBamFileService.getActiveBlockedBamsContainingSeqTracks([createSeqTrack()])
    }

    void "getActiveBlockedBamsContainingSeqTracks, empty list is properly handled"() {
        expect:
        [] ==  abstractBamFileService.getActiveBlockedBamsContainingSeqTracks([])
    }

    private SamplePair setSamplePairStatusToNeedProcessing_setup(SamplePair.ProcessingStatus processingStatus, String analysisName) {
        SamplePair samplePair = DomainFactory.createDisease(DomainFactory.createRoddyBamFile().workPackage)
        samplePair."${analysisName}ProcessingStatus" = processingStatus
        assert samplePair.save(flush: true)
        return samplePair
    }
}