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
    void testExecute() {
        RoddyBamFile roddyBamFile = testExecuteSetup()

        parseWgbsAlignmentQcJob.execute()

        assert TestCase.containSame(["8", "all", "7"], RoddySingleLaneQa.list()*.chromosome)
        assert TestCase.containSame(["8", "all", "7"], RoddyMergedBamQa.list()*.chromosome)
        assert TestCase.containSame(["8", "all", "7"], RoddyLibraryQa.list()*.chromosome)

        RoddySingleLaneQa.list().each { assert it.qcBasesMapped == SINGLE_LANE_QA_VALUE }
        RoddyMergedBamQa.list().each { assert it.qcBasesMapped == MERGED_QA_VALUE }
        RoddyLibraryQa.list().each { assert it.qcBasesMapped == LIBRARY_QA_VALUE }

        assert roddyBamFile.coverage != null
        assert roddyBamFile.coverageWithN != null
        assert roddyBamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
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

        File libraryQaFile = CollectionUtils.exactlyOneElement(roddyBamFile.workLibraryQAJsonFiles.values())
        File singleLaneQaFile = CollectionUtils.exactlyOneElement(roddyBamFile.workSingleLaneQAJsonFiles.values())
        File mergedQaFile = roddyBamFile.workMergedQAJsonFile

        DomainFactory.createQaFileOnFileSystem(libraryQaFile, LIBRARY_QA_VALUE)
        DomainFactory.createQaFileOnFileSystem(singleLaneQaFile, SINGLE_LANE_QA_VALUE)
        DomainFactory.createQaFileOnFileSystem(mergedQaFile, MERGED_QA_VALUE)

        ProcessingStep step = DomainFactory.createAndSaveProcessingStep(ParseWgbsAlignmentQcJob.class.toString(), roddyBamFile)
        parseWgbsAlignmentQcJob = context.getBean('parseWgbsAlignmentQcJob', step, null)

        return roddyBamFile
    }

}
