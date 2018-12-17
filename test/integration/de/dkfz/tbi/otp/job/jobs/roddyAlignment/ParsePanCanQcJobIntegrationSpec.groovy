package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import grails.test.spock.IntegrationSpec
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import de.dkfz.tbi.otp.utils.CollectionUtils

class ParsePanCanQcJobIntegrationSpec extends IntegrationSpec {

    @Autowired
    AbstractQualityAssessmentService abstractQualityAssessmentService

    @Rule
    TemporaryFolder temporaryFolder

    void "test execute ParsePanCanQcJob"() {
        given:
        File qaFile = temporaryFolder.newFile(RoddyBamFile.QUALITY_CONTROL_JSON_FILE_NAME)
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        SeqTrack seqTrack = CollectionUtils.exactlyOneElement(roddyBamFile.seqTracks)
        roddyBamFile.metaClass.getWorkMergedQAJsonFile = { -> qaFile }
        roddyBamFile.metaClass.getWorkSingleLaneQAJsonFiles = { -> [(seqTrack): qaFile] }

        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()
        DomainFactory.createReferenceGenomeEntries(referenceGenome, ['7', '8'])
        DomainFactory.createQaFileOnFileSystem(qaFile)

        ParsePanCanQcJob job = [
                getProcessParameterObject: { -> roddyBamFile },
        ] as ParsePanCanQcJob
        job.abstractQualityAssessmentService = abstractQualityAssessmentService
        job.qcTrafficLightService = new QcTrafficLightService()

        when:
        job.execute()

        then:
        CollectionUtils.containSame(["8", "all", "7"], RoddySingleLaneQa.list()*.chromosome)
        CollectionUtils.containSame(["8", "all", "7"], RoddyMergedBamQa.list()*.chromosome)
        roddyBamFile.coverage != null
        roddyBamFile.coverageWithN != null
        roddyBamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
        roddyBamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
        roddyBamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
    }
}
