package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.ngsdata.*
import spock.lang.*

class AbstractMergedBamFileServiceIntegrationSpec extends Specification {

    AbstractMergedBamFileService abstractMergedBamFileService

    final String MERGED_BAM_FILES_PATH = "merged-alignment"

    def "destination directory of ProcessedMergedBamFile"() {
        given:
        ProcessedMergedBamFile mergedBamFile = DomainFactory.createProcessedMergedBamFile()
        Realm realm = DomainFactory.createRealmDataManagement(
                name: mergedBamFile.project.realmName,
        )
        String destinationExp = expectedMergedAlignmentPath(mergedBamFile, realm)

        when:
        String destinationAct = AbstractMergedBamFileService.destinationDirectory(mergedBamFile)

        then:
        destinationExp == destinationAct
    }

    def "destination directory of RoddyBamFile"() {
        given:
        RoddyBamFile mergedBamFile = DomainFactory.createRoddyBamFile()
        Realm realm = DomainFactory.createRealmDataManagement(
                name: mergedBamFile.project.realmName,
        )
        String destinationExp = expectedMergedAlignmentPath(mergedBamFile, realm)

        when:
        String destinationAct = AbstractMergedBamFileService.destinationDirectory(mergedBamFile)

        then:
        destinationExp == destinationAct
    }

    private String expectedMergedAlignmentPath(AbstractMergedBamFile mergedBamFile, Realm realm) {
        String pidPath = "${realm.rootPath}/${mergedBamFile.project.dirName}/sequencing/${mergedBamFile.seqType.dirName}/view-by-pid/${mergedBamFile.individual.pid}"
        return "${pidPath}/${mergedBamFile.sampleType.dirName}/${mergedBamFile.seqType.libraryLayoutDirName}/${MERGED_BAM_FILES_PATH}/"
    }

    def "set SamplePairStatus to need processing while input is null"() {
        when:
        abstractMergedBamFileService.setSamplePairStatusToNeedProcessing(null)

        then:
        AssertionError error = thrown()
        error.message.contains("The input bam file must not be null")
    }

    @Unroll
    def "set #analysisName samplePairStatus to need processing while samplePair is in state needs processing"() {
        given:
        SamplePair samplePair = setSamplePairStatusToNeedProcessing_setup(ProcessingStatus.NEEDS_PROCESSING, analysisName)
        AbstractMergedBamFile bamFile = AbstractMergedBamFile.findByWorkPackage(samplePair.mergingWorkPackage2)

        when:
        abstractMergedBamFileService.setSamplePairStatusToNeedProcessing(bamFile)

        then:
        samplePair."${analysisName}ProcessingStatus" == ProcessingStatus.NEEDS_PROCESSING

        where:
        analysisName << [
                "aceseq",
                "indel",
                "snv",
                "sophia",
        ]
    }

    @Unroll
    def "set #analysisName samplePairStatus to need processing while samplePair is in state no processing needed"() {
        given:
        SamplePair samplePair = setSamplePairStatusToNeedProcessing_setup(ProcessingStatus.NO_PROCESSING_NEEDED, analysisName)
        AbstractMergedBamFile bamFile = AbstractMergedBamFile.findByWorkPackage(samplePair.mergingWorkPackage2)

        when:
        abstractMergedBamFileService.setSamplePairStatusToNeedProcessing(bamFile)

        then:
        samplePair."${analysisName}ProcessingStatus" == ProcessingStatus.NEEDS_PROCESSING

        where:
        analysisName << [
                "aceseq",
                "indel",
                "snv",
                "sophia",
        ]
    }

    @Unroll
    def "set #analysisName samplePair status to need processing while other samplePair in state no processing needed"() {
        given:
        SamplePair samplePair1 = setSamplePairStatusToNeedProcessing_setup(ProcessingStatus.NO_PROCESSING_NEEDED, analysisName)
        SamplePair samplePair2 = setSamplePairStatusToNeedProcessing_setup(ProcessingStatus.NEEDS_PROCESSING, analysisName)
        AbstractMergedBamFile bamFile = AbstractMergedBamFile.findByWorkPackage(samplePair2.mergingWorkPackage2)

        when:
        abstractMergedBamFileService.setSamplePairStatusToNeedProcessing(bamFile)

        then:
        samplePair1."${analysisName}ProcessingStatus" == ProcessingStatus.NO_PROCESSING_NEEDED

        where:
        analysisName << [
                "indel",
                "snv",
        ]
    }

    private SamplePair setSamplePairStatusToNeedProcessing_setup(ProcessingStatus processingStatus, String analysisName) {
        SamplePair samplePair = DomainFactory.createDisease(DomainFactory.createRoddyBamFile().workPackage)
        samplePair."${analysisName}ProcessingStatus" = processingStatus
        assert samplePair.save(flush: true)
        return samplePair
    }
}
