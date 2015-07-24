package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import static de.dkfz.tbi.otp.dataprocessing.AbstractQualityAssessmentServiceTests.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractQualityAssessmentService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyMergedBamQa
import de.dkfz.tbi.otp.dataprocessing.RoddySingleLaneQa
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack

class ParsePanCanQcJobTests {

    @Autowired
    AbstractQualityAssessmentService abstractQualityAssessmentService

    @Rule
    public TemporaryFolder temporaryFolder

    @Test
    void testExecute() {
        temporaryFolder = new TemporaryFolder()
        temporaryFolder.create()
        File qaFile = temporaryFolder.newFile(RoddyBamFile.QUALITY_CONTROL_JSON_FILE_NAME)
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        SeqTrack seqTrack = exactlyOneElement(roddyBamFile.seqTracks)
        roddyBamFile.metaClass.getTmpRoddyMergedQAJsonFile = { -> qaFile }
        roddyBamFile.metaClass.getTmpRoddySingleLaneQAJsonFiles = { -> [(seqTrack): qaFile] }
        createReferenceGenomeEntriesAndQaFileOnFilesystem(roddyBamFile.referenceGenome, qaFile)
        ParsePanCanQcJob job = [
                getProcessParameterObject: { -> roddyBamFile },
        ] as ParsePanCanQcJob
        job.abstractQualityAssessmentService = abstractQualityAssessmentService

        job.execute()

        assert TestCase.containSame(["8", "all", "7"], RoddySingleLaneQa.list()*.chromosome)
        assert TestCase.containSame(["8", "all", "7"], RoddyMergedBamQa.list()*.chromosome)
        assert roddyBamFile.coverage != null
        assert roddyBamFile.coverageWithN != null
    }
}
