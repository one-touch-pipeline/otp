package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.*
import grails.test.spock.*
import org.codehaus.groovy.grails.commons.spring.*
import org.junit.*
import org.junit.rules.*
import org.springframework.beans.factory.annotation.*
import spock.lang.Unroll

class ParseWgbsAlignmentQcJobIntegrationSpec extends IntegrationSpec {

    @Autowired
    GrailsApplicationContext context

    ParseWgbsAlignmentQcJob parseWgbsAlignmentQcJob

    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    QcTrafficLightService qcTrafficLightService
    TestConfigService configService

    final static String LIBRARY_NAME = "library12"
    final static String NORMALIZED_LIBRARY_NAME = "12"
    final static List EXPECTED_CHROMOSOME_LIST = ["8", "all", "7"]

    RoddyBamFile roddyBamFile

    void setup() {
        configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])

        qcTrafficLightService = new QcTrafficLightService()
        qcTrafficLightService.commentService = new CommentService()
        qcTrafficLightService.commentService.springSecurityService = Mock(SpringSecurityService) {
            getPrincipal() >> { new Principal(username: "dummy") }
        }

        roddyBamFile = DomainFactory.createRoddyBamFile()

        SeqTrack seqTrack = CollectionUtils.exactlyOneElement(roddyBamFile.seqTracks)
        seqTrack.libraryName = LIBRARY_NAME
        seqTrack.normalizedLibraryName = NORMALIZED_LIBRARY_NAME
        assert seqTrack.save(flush: true)

        ProcessingStep step = DomainFactory.createAndSaveProcessingStep(ParseWgbsAlignmentQcJob.class.toString(), roddyBamFile)
        parseWgbsAlignmentQcJob = context.getBean('parseWgbsAlignmentQcJob')
        parseWgbsAlignmentQcJob.processingStep = step
    }

    void cleanup() {
        configService.clean()
    }

    @Unroll
    void "testExecute, no RoddyLibraryQa created when #seqTrackNumber SeqTracks have a common library name"() {
        given:
        MergingWorkPackage workPackage = roddyBamFile.mergingWorkPackage
        while (roddyBamFile.getContainedSeqTracks().size() < seqTrackNumber) {
            SeqTrack seqTrack = DomainFactory.createSeqTrackWithDataFiles(workPackage, [
                    libraryName          : LIBRARY_NAME,
                    normalizedLibraryName: NORMALIZED_LIBRARY_NAME,
            ])

            roddyBamFile.seqTracks.add(seqTrack)
            roddyBamFile.numberOfMergedLanes++
            assert roddyBamFile.save(flush: true)
        }

        createAllQaFilesOnFileSystem(roddyBamFile)

        when:
        parseWgbsAlignmentQcJob.execute()

        then:
        List<String> chromosomes = RoddySingleLaneQa.list()*.chromosome
        chromosomes.size() == seqTrackNumber * 3
        CollectionUtils.containSame(EXPECTED_CHROMOSOME_LIST, chromosomes as Set)

        RoddyLibraryQa.list().isEmpty()

        validateCommonExecutionResults(roddyBamFile)

        where:
        seqTrackNumber << [1, 2, 3]
    }

    void "testExecute, RoddyLibraryQa created when SeqTracks have different library names"() {
        String libraryName = "library14"
        MergingWorkPackage workPackage = roddyBamFile.mergingWorkPackage
        SeqTrack secondSeqTrack = DomainFactory.createSeqTrackWithDataFiles(workPackage, [
                libraryName          : libraryName,
                normalizedLibraryName: SeqTrack.normalizeLibraryName(libraryName),
        ])

        roddyBamFile.seqTracks.add(secondSeqTrack)
        roddyBamFile.numberOfMergedLanes = roddyBamFile.getContainedSeqTracks().size()
        assert roddyBamFile.save(flush: true)

        createAllQaFilesOnFileSystem(roddyBamFile)

        when:
        parseWgbsAlignmentQcJob.execute()

        then:
        List<RoddySingleLaneQa> roddySingleLaneQaList = RoddySingleLaneQa.list()
        List<RoddyLibraryQa> roddyLibraryQaList = RoddyLibraryQa.list()

        [roddySingleLaneQaList, roddyLibraryQaList].every { it.size() == 3 * roddyBamFile.seqTracks.size() }

        CollectionUtils.containSame(EXPECTED_CHROMOSOME_LIST, roddySingleLaneQaList*.chromosome as Set)
        CollectionUtils.containSame(EXPECTED_CHROMOSOME_LIST, roddyLibraryQaList*.chromosome as Set)

        RoddyLibraryQa.list()

        validateCommonExecutionResults(roddyBamFile)
    }

    @Unroll
    void "testExecute, libraryName=#libraryName does not throw exception"() {
        given:
        SeqTrack seqTrack = CollectionUtils.exactlyOneElement(roddyBamFile.seqTracks)
        seqTrack.libraryName = libraryName
        seqTrack.normalizedLibraryName = libraryName

        createAllQaFilesOnFileSystem(roddyBamFile)

        when:
        parseWgbsAlignmentQcJob.execute()

        then:
        CollectionUtils.containSame(EXPECTED_CHROMOSOME_LIST, RoddySingleLaneQa.list()*.chromosome)
        RoddyLibraryQa.list().isEmpty()

        validateCommonExecutionResults(roddyBamFile)

        where:
        libraryName << [null, ""]
    }

    private void validateCommonExecutionResults(RoddyBamFile roddyBamFile) {
        assert RoddySingleLaneQa.list()
        assert RoddyMergedBamQa.list()

        assert CollectionUtils.containSame(EXPECTED_CHROMOSOME_LIST, RoddyMergedBamQa.list()*.chromosome)

        assert roddyBamFile.coverage != null
        assert roddyBamFile.coverageWithN != null
        assert roddyBamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
        assert roddyBamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
    }

    private void createAllQaFilesOnFileSystem(RoddyBamFile roddyBamFile) {
        roddyBamFile.workSingleLaneQAJsonFiles.values().each {
            DomainFactory.createQaFileOnFileSystem(it, 111111)
        }
        roddyBamFile.workLibraryQAJsonFiles.values().each {
            DomainFactory.createQaFileOnFileSystem(it, 222222)
        }
        DomainFactory.createQaFileOnFileSystem(roddyBamFile.workMergedQAJsonFile, 333333)
    }
}
