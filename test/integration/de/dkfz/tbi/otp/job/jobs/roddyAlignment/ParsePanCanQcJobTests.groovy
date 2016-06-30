package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import de.dkfz.tbi.otp.utils.HelperUtils
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.beans.factory.annotation.Autowired


import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class ParsePanCanQcJobTests {

    @Autowired
    AbstractQualityAssessmentService abstractQualityAssessmentService

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test
    void testExecute() {
        File qaFile = temporaryFolder.newFile(RoddyBamFile.QUALITY_CONTROL_JSON_FILE_NAME)
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        SeqTrack seqTrack = exactlyOneElement(roddyBamFile.seqTracks)
        roddyBamFile.metaClass.getWorkMergedQAJsonFile = { -> qaFile }
        roddyBamFile.metaClass.getWorkSingleLaneQAJsonFiles = { -> [(seqTrack): qaFile] }

        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()
        DomainFactory.createReferenceGenomeEntries(referenceGenome, ['7', '8'])
        DomainFactory.createQaFileOnFileSystem(qaFile)

        ParsePanCanQcJob job = [
                getProcessParameterObject: { -> roddyBamFile },
        ] as ParsePanCanQcJob
        job.abstractQualityAssessmentService = abstractQualityAssessmentService
        job.execute()

        assert TestCase.containSame(["8", "all", "7"], RoddySingleLaneQa.list()*.chromosome)
        assert TestCase.containSame(["8", "all", "7"], RoddyMergedBamQa.list()*.chromosome)
        assert roddyBamFile.coverage != null
        assert roddyBamFile.coverageWithN != null
        assert roddyBamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
    }
}
