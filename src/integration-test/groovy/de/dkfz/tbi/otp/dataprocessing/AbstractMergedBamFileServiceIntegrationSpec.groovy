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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack

@Rollback
@Integration
class AbstractMergedBamFileServiceIntegrationSpec extends Specification implements IsRoddy {

    AbstractMergedBamFileService abstractMergedBamFileService

    final String MERGED_BAM_FILES_PATH = "merged-alignment"

    static final List<String> processingSteps = [
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
        analysisPipelines.size() == processingSteps.size()
    }

    void "destination directory of ProcessedMergedBamFile"() {
        given:
        ProcessedMergedBamFile mergedBamFile = DomainFactory.createProcessedMergedBamFile()
        String destinationExp = expectedMergedAlignmentPath(mergedBamFile)

        when:
        String destinationAct = AbstractMergedBamFileService.destinationDirectory(mergedBamFile)

        then:
        destinationExp == destinationAct
    }

    void "destination directory of RoddyBamFile"() {
        given:
        RoddyBamFile mergedBamFile = DomainFactory.createRoddyBamFile()
        String destinationExp = expectedMergedAlignmentPath(mergedBamFile)

        when:
        String destinationAct = AbstractMergedBamFileService.destinationDirectory(mergedBamFile)

        then:
        destinationExp == destinationAct
    }

    private String expectedMergedAlignmentPath(AbstractMergedBamFile mergedBamFile) {
        String pidPath = "${TestConfigService.getInstance().getRootPath()}/${mergedBamFile.project.dirName}/sequencing/${mergedBamFile.seqType.dirName}/view-by-pid/${mergedBamFile.individual.pid}"
        return "${pidPath}/${mergedBamFile.sampleType.dirName}/${mergedBamFile.seqType.libraryLayoutDirName}/${MERGED_BAM_FILES_PATH}/"
    }

    void "set SamplePairStatus to need processing while input is null"() {
        when:
        abstractMergedBamFileService.setSamplePairStatusToNeedProcessing(null)

        then:
        AssertionError error = thrown()
        error.message.contains("The input bam file must not be null")
    }

    @Unroll
    void "set #analysisName samplePairStatus to need processing while samplePair is in state needs processing"() {
        given:
        SamplePair samplePair = setSamplePairStatusToNeedProcessing_setup(ProcessingStatus.NEEDS_PROCESSING, analysisName)
        AbstractMergedBamFile bamFile = AbstractMergedBamFile.findByWorkPackage(samplePair.mergingWorkPackage2)

        when:
        abstractMergedBamFileService.setSamplePairStatusToNeedProcessing(bamFile)

        then:
        samplePair."${analysisName}ProcessingStatus" == ProcessingStatus.NEEDS_PROCESSING

        where:
        analysisName << processingSteps
    }

    @Unroll
    void "set #analysisName samplePairStatus to need processing while samplePair is in state no processing needed"() {
        given:
        DomainFactory.createAllAnalysableSeqTypes()
        SamplePair samplePair = setSamplePairStatusToNeedProcessing_setup(ProcessingStatus.NO_PROCESSING_NEEDED, analysisName)
        AbstractMergedBamFile bamFile = AbstractMergedBamFile.findByWorkPackage(samplePair.mergingWorkPackage2)

        when:
        abstractMergedBamFileService.setSamplePairStatusToNeedProcessing(bamFile)

        then:
        samplePair."${analysisName}ProcessingStatus" == ProcessingStatus.NEEDS_PROCESSING

        where:
        analysisName << processingSteps
    }

    @Unroll
    void "set #analysisName samplePair status to need processing while other samplePair in state no processing needed"() {
        given:
        SamplePair samplePair1 = setSamplePairStatusToNeedProcessing_setup(ProcessingStatus.NO_PROCESSING_NEEDED, analysisName)
        SamplePair samplePair2 = setSamplePairStatusToNeedProcessing_setup(ProcessingStatus.NEEDS_PROCESSING, analysisName)
        AbstractMergedBamFile bamFile = AbstractMergedBamFile.findByWorkPackage(samplePair2.mergingWorkPackage2)

        when:
        abstractMergedBamFileService.setSamplePairStatusToNeedProcessing(bamFile)

        then:
        samplePair1."${analysisName}ProcessingStatus" == ProcessingStatus.NO_PROCESSING_NEEDED

        where:
        analysisName << processingSteps
    }

    void "getActiveBlockedBamsContainingSeqTracks, only returns not withdrawn blocked bams"() {
        given:
        Closure<RoddyBamFile> createRoddyBamFileHelper = { boolean blocked, boolean withdrawn ->
            Map props = [:]
            if (blocked) {
                props << [qcTrafficLightStatus: AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED]
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
        ].collect {
            it.containedSeqTracks
        }.flatten()

        when:
        List<AbstractMergedBamFile> bams = abstractMergedBamFileService.getActiveBlockedBamsContainingSeqTracks(seqTracks)

        then:
        bams.size() == 1
    }

    void "getActiveBlockedBamsContainingSeqTracks, empty merging work package list is properly handled"() {
        expect:
        [] == abstractMergedBamFileService.getActiveBlockedBamsContainingSeqTracks([createSeqTrack()])
    }

    void "getActiveBlockedBamsContainingSeqTracks, empty list is properly handled"() {
        expect:
        [] ==  abstractMergedBamFileService.getActiveBlockedBamsContainingSeqTracks([])
    }

    private SamplePair setSamplePairStatusToNeedProcessing_setup(ProcessingStatus processingStatus, String analysisName) {
        SamplePair samplePair = DomainFactory.createDisease(DomainFactory.createRoddyBamFile().workPackage)
        samplePair."${analysisName}ProcessingStatus" = processingStatus
        assert samplePair.save(flush: true)
        return samplePair
    }
}
