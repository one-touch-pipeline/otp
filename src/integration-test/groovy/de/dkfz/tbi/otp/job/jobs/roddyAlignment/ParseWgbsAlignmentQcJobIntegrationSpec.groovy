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
package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import grails.core.GrailsApplication
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
class ParseWgbsAlignmentQcJobIntegrationSpec extends Specification {

    @Autowired
    GrailsApplication grailsApplication

    ParseWgbsAlignmentQcJob parseWgbsAlignmentQcJob

    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    QcTrafficLightService qcTrafficLightService
    TestConfigService configService

    final static String LIBRARY_NAME = "library12"
    final static String NORMALIZED_LIBRARY_NAME = "12"
    final static List EXPECTED_CHROMOSOME_LIST = ["8", "all", "7"]

    RoddyBamFile roddyBamFile

    void setupData() {
        configService.addOtpProperties(temporaryFolder.newFolder().toPath())

        qcTrafficLightService = new QcTrafficLightService()
        qcTrafficLightService.commentService = new CommentService()
        qcTrafficLightService.commentService.securityService = Mock(SecurityService) {
            getCurrentUser() >> { new User(username: "dummy") }
        }

        roddyBamFile = DomainFactory.createRoddyBamFile()

        SeqTrack seqTrack = CollectionUtils.exactlyOneElement(roddyBamFile.seqTracks)
        seqTrack.libraryName = LIBRARY_NAME
        seqTrack.normalizedLibraryName = NORMALIZED_LIBRARY_NAME
        assert seqTrack.save(flush: true)

        ProcessingStep step = DomainFactory.createAndSaveProcessingStep(ParseWgbsAlignmentQcJob.toString(), roddyBamFile)
        parseWgbsAlignmentQcJob = grailsApplication.mainContext.getBean('parseWgbsAlignmentQcJob')
        parseWgbsAlignmentQcJob.processingStep = step
    }

    void cleanup() {
        configService.clean()
    }

    @Unroll
    void "testExecute, no RoddyLibraryQa created when #seqTrackNumber SeqTracks have a common library name"() {
        given:
        setupData()

        MergingWorkPackage workPackage = roddyBamFile.mergingWorkPackage
        while (roddyBamFile.containedSeqTracks.size() < seqTrackNumber) {
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
        given:
        setupData()

        String libraryName = "library14"
        MergingWorkPackage workPackage = roddyBamFile.mergingWorkPackage
        SeqTrack secondSeqTrack = DomainFactory.createSeqTrackWithDataFiles(workPackage, [
                libraryName          : libraryName,
                normalizedLibraryName: SeqTrack.normalizeLibraryName(libraryName),
        ])

        roddyBamFile.seqTracks.add(secondSeqTrack)
        roddyBamFile.numberOfMergedLanes = roddyBamFile.containedSeqTracks.size()
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
        setupData()

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
