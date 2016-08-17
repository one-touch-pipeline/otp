package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CollectionUtils
import org.codehaus.groovy.grails.commons.spring.GrailsApplicationContext
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.beans.factory.annotation.Autowired

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class ParseWgbsAlignmentQcJobTests {

    final long LIBRARY_QA_VALUE = 1111111
    final long SINGLE_LANE_QA_VALUE = 222222
    final long MERGED_QA_VALUE = 3333333


    @Autowired
    GrailsApplicationContext context

    ParseWgbsAlignmentQcJob parseWgbsAlignmentQcJob


    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test
    void testExecute_OnlyOneLibraryInMergedBamFile() {
        RoddyBamFile roddyBamFile = testExecuteSetup()
        File singleLaneQaFile = CollectionUtils.exactlyOneElement(roddyBamFile.workSingleLaneQAJsonFiles.values())
        DomainFactory.createQaFileOnFileSystem(singleLaneQaFile, SINGLE_LANE_QA_VALUE)

        parseWgbsAlignmentQcJob.execute()

        assert TestCase.containSame(["8", "all", "7"], RoddySingleLaneQa.list()*.chromosome)
        assert RoddyLibraryQa.list().isEmpty()

        validateCommonExecutionResults(roddyBamFile)
    }

    @Test
    void testExecute_HandleLibraryNameNullAndEmptyStringCorrect() {
        RoddyBamFile roddyBamFile = testExecuteSetup()
        SeqTrack seqTrack = exactlyOneElement(roddyBamFile.seqTracks)
        seqTrack.libraryName = ""
        seqTrack.normalizedLibraryName = ""
        assert seqTrack.save(flush: true)

        MergingWorkPackage workPackage = roddyBamFile.mergingWorkPackage

        SeqTrack seqTrack2 = DomainFactory.createSeqTrackWithDataFiles(workPackage, [
                libraryName: null,
                normalizedLibraryName: null,
        ])
        assert seqTrack2.save(flush: true)

        roddyBamFile.seqTracks.add(seqTrack2)
        roddyBamFile.numberOfMergedLanes = 2
        assert roddyBamFile.save(flush: true)

        roddyBamFile.workSingleLaneQAJsonFiles.values().each {
            DomainFactory.createQaFileOnFileSystem(it, SINGLE_LANE_QA_VALUE)
        }

        parseWgbsAlignmentQcJob.execute()

        TestCase.assertContainSame(["8", "all", "7", "8", "all", "7"] as Set, RoddySingleLaneQa.list()*.chromosome as Set)
        assert RoddyLibraryQa.list().isEmpty()

        validateCommonExecutionResults(roddyBamFile)
    }

    @Test
    void testExecute_TwoLibrariesInMergedBamFile() {
        RoddyBamFile roddyBamFile = testExecuteSetup()

        MergingWorkPackage workPackage = roddyBamFile.mergingWorkPackage

        SeqTrack seqTrack2 = DomainFactory.createSeqTrackWithDataFiles(workPackage, [libraryName: 'library14', normalizedLibraryName: SeqTrack.normalizeLibraryName('library14')])
        assert seqTrack2.save(flush: true)

        roddyBamFile.seqTracks.add(seqTrack2)
        roddyBamFile.numberOfMergedLanes = 2
        assert roddyBamFile.save(flush: true)

        roddyBamFile.workLibraryQAJsonFiles.values().each {
            DomainFactory.createQaFileOnFileSystem(it, LIBRARY_QA_VALUE)
        }

        roddyBamFile.workSingleLaneQAJsonFiles.values().each {
            DomainFactory.createQaFileOnFileSystem(it, SINGLE_LANE_QA_VALUE)
        }

        parseWgbsAlignmentQcJob.execute()

        List expectedElements = ["8", "all", "7", "8", "all", "7"]
        List actualSingleLaneElements = RoddySingleLaneQa.list()*.chromosome
        List actualLibraryElements = RoddyLibraryQa.list()*.chromosome

        assert expectedElements.size() == actualSingleLaneElements.size()
        assert expectedElements.size() == actualLibraryElements.size()

        assert TestCase.containSame(expectedElements as Set, actualSingleLaneElements as Set)
        assert TestCase.containSame(expectedElements as Set, actualLibraryElements as Set)

        RoddyLibraryQa.list().each { assert it.qcBasesMapped == LIBRARY_QA_VALUE }

        validateCommonExecutionResults(roddyBamFile)
    }

    private RoddyBamFile testExecuteSetup() {
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        SeqTrack seqTrack = exactlyOneElement(roddyBamFile.seqTracks)
        seqTrack.libraryName = "library12"
        seqTrack.normalizedLibraryName = "12"
        assert seqTrack.save(flush: true)

        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: roddyBamFile.project.realmName])

        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()
        DomainFactory.createReferenceGenomeEntries(referenceGenome, ['7', '8'])

        File mergedQaFile = roddyBamFile.workMergedQAJsonFile
        DomainFactory.createQaFileOnFileSystem(mergedQaFile, MERGED_QA_VALUE)

        ProcessingStep step = DomainFactory.createAndSaveProcessingStep(ParseWgbsAlignmentQcJob.class.toString(), roddyBamFile)
        parseWgbsAlignmentQcJob = context.getBean('parseWgbsAlignmentQcJob')
        parseWgbsAlignmentQcJob.processingStep = step

        return roddyBamFile
    }

    private void validateCommonExecutionResults(RoddyBamFile roddyBamFile){
        RoddySingleLaneQa.list().each { assert it.qcBasesMapped == SINGLE_LANE_QA_VALUE }
        RoddyMergedBamQa.list().each { assert it.qcBasesMapped == MERGED_QA_VALUE }

        assert TestCase.containSame(["8", "all", "7"], RoddyMergedBamQa.list()*.chromosome)

        assert roddyBamFile.coverage != null
        assert roddyBamFile.coverageWithN != null
        assert roddyBamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
    }

}
