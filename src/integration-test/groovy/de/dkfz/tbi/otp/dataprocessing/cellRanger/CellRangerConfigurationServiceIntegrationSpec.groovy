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
package de.dkfz.tbi.otp.dataprocessing.cellRanger

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import grails.validation.ValidationException
import org.springframework.validation.Errors
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfigurationService.CellRangerMwpParameter
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.sql.Timestamp
import java.time.LocalDate

@Rollback
@Integration
class CellRangerConfigurationServiceIntegrationSpec extends Specification implements CellRangerFactory, UserAndRoles {

    CellRangerConfigurationService cellRangerConfigurationService
    Project project
    SeqType seqType
    Individual individual
    SampleType sampleType
    Sample sampleA
    Sample sampleB
    SeqTrack seqTrackA
    SeqTrack seqTrackB
    ReferenceGenomeIndex referenceGenomeIndex
    User requester

    CellRangerMwpParameter createCellRangerMwpParameter(Map properties = [:]) {
        return new CellRangerMwpParameter([
                expectedCells       : 1,
                enforcedCells       : null,
                referenceGenomeIndex: referenceGenomeIndex,
                seqType             : seqType,
        ] + properties)
    }

    private static boolean mwpUsesParameter(CellRangerMergingWorkPackage mwp, CellRangerMwpParameter parameter) {
        return mwp.expectedCells == parameter.expectedCells &&
                mwp.enforcedCells == parameter.enforcedCells &&
                mwp.referenceGenomeIndex == parameter.referenceGenomeIndex &&
                mwp.seqType == parameter.seqType
    }

    void setupData() {
        createUserAndRoles()
        requester = DomainFactory.createUser()

        project = createProject()
        seqType = createSeqType()
        referenceGenomeIndex = createReferenceGenomeIndex()
        Pipeline pipeline = findOrCreatePipeline()
        createConfig(project: project, seqType: seqType, pipeline: pipeline)

        createMergingCriteria(project: project, seqType: seqType)

        individual = createIndividual(project: project)
        sampleType = createSampleType()
        sampleA = createSample(individual: individual, sampleType: sampleType)
        seqTrackA = createSeqTrack(seqType: seqType, sample: sampleA)
        sampleA.refresh()

        Individual individualB = createIndividual(project: project)
        SampleType sampleTypeB = createSampleType()
        sampleB = createSample(individual: individualB, sampleType: sampleTypeB)
        seqTrackB = createSeqTrack(seqType: seqType, sample: sampleB)
        sampleB.refresh()
    }

    /**
     * Returns a list of SeqTracks with LibPrepKits and SeqPlatformGroups in such a way that
     * they will be split into 5 MergingWorkPackages for processing.
     */
    List<SeqTrack> setupMultipleSeqTracksOfDifferentSeqPlatformGroupsAndLibPrepKits() {
        List<LibraryPreparationKit> libPrepKits = (1..4).collect { createLibraryPreparationKit() }

        List<SeqPlatform> seqPlatforms = (1..4).collect { createSeqPlatformWithSeqPlatformGroup() }

        Map<SeqPlatform, Run> runBySeqPlatform = seqPlatforms.collectEntries { SeqPlatform seqPlatform ->
            [(seqPlatform): createRun(seqPlatform: seqPlatform)]
        }

        Closure<SeqTrack> createSeqTrackClosure = { SeqPlatform seqPlatform, LibraryPreparationKit libPreparationKit ->
            Run run = runBySeqPlatform[seqPlatform]
            return createSeqTrack(sample: sampleA, seqType: seqType, run: run, libraryPreparationKit: libPreparationKit)
        }

        List<SeqTrack> seqTracks = [
                createSeqTrackClosure(seqPlatforms[0], libPrepKits[0]),
                createSeqTrackClosure(seqPlatforms[0], libPrepKits[0]),

                createSeqTrackClosure(seqPlatforms[1], libPrepKits[0]),

                createSeqTrackClosure(seqPlatforms[2], libPrepKits[1]),
                createSeqTrackClosure(seqPlatforms[2], libPrepKits[1]),

                createSeqTrackClosure(seqPlatforms[3], libPrepKits[2]),
                createSeqTrackClosure(seqPlatforms[3], libPrepKits[2]),

                createSeqTrackClosure(seqPlatforms[3], libPrepKits[3]),
        ]
        sampleA.refresh()
        return seqTracks
    }

    void "test getSamples"() {
        given:
        setupData()

        when:
        CellRangerConfigurationService.Samples samples = SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.getSamples(project, individual, sampleType)
        }

        then:
        TestCase.assertContainSame(samples.allSamples, [sampleA, sampleB])
        TestCase.assertContainSame(samples.selectedSamples, [sampleA])
    }

    void "test getSamples for whole project"() {
        given:
        setupData()

        when:
        CellRangerConfigurationService.Samples samples = SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.getSamples(project, null, null)
        }

        then:
        TestCase.assertContainSame(samples.allSamples, [sampleA, sampleB])
        TestCase.assertContainSame(samples.selectedSamples, [sampleA, sampleB])
    }

    void "test getSeqTracksGroupedByPlatformGroupAndKit properly groups SeqTracks by SeqPlatformGroup and LibPrepKit"() {
        given:
        setupData()
        List<SeqTrack> seqTracks = setupMultipleSeqTracksOfDifferentSeqPlatformGroupsAndLibPrepKits()

        when:
        Map<CellRangerConfigurationService.PlatformGroupAndKit, List<SeqTrack>> result = cellRangerConfigurationService.getSeqTracksGroupedByPlatformGroupAndKit(seqTracks)

        then:
        result.size() == 5
        result.every { CellRangerConfigurationService.PlatformGroupAndKit key, List<SeqTrack> seqTracksOfKey ->
            seqTracks.findAll {
                key.seqPlatformGroup == it.seqPlatformGroup && key.libraryPreparationKit == it.libraryPreparationKit
            } == seqTracksOfKey
        }
    }

    void "prepareCellRangerExecution creates MWPs and resets Tickets"() {
        given:
        setupData()
        FastqImportInstance fastqImportInstance = createFastqImportInstance(
                dataFiles: [
                        createDataFile(seqTrack: seqTrackA),
                        createDataFile(seqTrack: seqTrackB),
                ] as Set<DataFile>,
                otrsTicket: createOtrsTicketWithEndDatesAndNotificationSent(),
        )
        CellRangerMwpParameter expectedParameter = createCellRangerMwpParameter()

        when:
        Errors errors = SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.prepareCellRangerExecution(
                    expectedParameter.expectedCells,
                    expectedParameter.enforcedCells,
                    expectedParameter.referenceGenomeIndex,
                    project,
                    individual,
                    sampleType,
                    expectedParameter.seqType
            )
        }

        then: "no errors"
        !errors

        and: "MWPs created"
        CellRangerMergingWorkPackage mwp = CollectionUtils.exactlyOneElement(CellRangerMergingWorkPackage.all)
        mwpUsesParameter(mwp, expectedParameter)

        and: "affected ticket reset"
        fastqImportInstance.with {
            !otrsTicket.finalNotificationSent
            !otrsTicket.alignmentFinished
        }
    }

    void "resetTicketForCellRangerExecution properly prepares ticket"() {
        given:
        OtrsTicket otrsTicket = createOtrsTicket(
                finalNotificationSent: finalNotification,
                alignmentFinished: finished ? new Date() : null,
        )

        when:
        cellRangerConfigurationService.resetTicketForCellRangerExecution(otrsTicket)

        then:
        !otrsTicket.finalNotificationSent
        !otrsTicket.alignmentFinished

        where:
        finalNotification | finished
        false             | true
        true              | false
        true              | true
    }

    void "resetAllTicketsOfSeqTracksForCellRangerExecution, reset only affected Tickets"() {
        given:
        setupData()

        Closure<FastqImportInstance> createImportInstanceHelper = {
            return createFastqImportInstance(
                    dataFiles: [createDataFile()] as Set<DataFile>,
                    otrsTicket: createOtrsTicketWithEndDatesAndNotificationSent(),
            )
        }

        FastqImportInstance instanceResetA = createImportInstanceHelper()
        FastqImportInstance instanceResetB = createImportInstanceHelper()
        FastqImportInstance instanceUntouched = createImportInstanceHelper()

        Set<SeqTrack> seqTracks = [instanceResetA, instanceResetB].collectMany { it.dataFiles }*.seqTrack as Set<SeqTrack>

        when:
        cellRangerConfigurationService.resetAllTicketsOfSeqTracksForCellRangerExecution(seqTracks)

        then:
        instanceResetA.with {
            !otrsTicket.finalNotificationSent
            !otrsTicket.alignmentFinished
        }
        instanceResetB.with {
            !otrsTicket.finalNotificationSent
            !otrsTicket.alignmentFinished
        }
        instanceUntouched.with {
            otrsTicket.finalNotificationSent
            otrsTicket.alignmentFinished
        }
    }

    void "test createMergingWorkPackagesForSamples creates MWPs for multiple samples"() {
        given:
        setupData()
        CellRangerMwpParameter parameter = createCellRangerMwpParameter()
        List<Sample> samples = [sampleA, sampleB]

        when:
        cellRangerConfigurationService.createMergingWorkPackagesForSamples(samples, parameter, requester)

        then:
        List<CellRangerMergingWorkPackage> all = CellRangerMergingWorkPackage.all
        CollectionUtils.containSame(all*.sample, samples)
        all.size() == 2
        all.every { CellRangerMergingWorkPackage mwp ->
            mwpUsesParameter(mwp, parameter)
        }
    }

    @Unroll
    void "test createMergingWorkPackagesForSample (expectedCells=#expectedCells, enforcedCells=#enforcedCells)"() {
        given:
        setupData()
        CellRangerMwpParameter parameter = createCellRangerMwpParameter(expectedCells: expectedCells, enforcedCells: enforcedCells)

        when:
        cellRangerConfigurationService.findAllMergingWorkPackagesBySamplesAndPipeline(sampleA, parameter, requester)

        then:
        CellRangerMergingWorkPackage mwp = CollectionUtils.exactlyOneElement(CellRangerMergingWorkPackage.all)
        mwp.sample == sampleA
        mwp.seqTracks == [seqTrackA] as Set
        mwp.project == project
        mwp.individual == individual
        mwpUsesParameter(mwp, parameter)

        where:
        expectedCells | enforcedCells
        5000          | null
        null          | 5000
        null          | null
    }

    void "test createMergingWorkPackagesForSample creates MWP where DELETED MWP exists"() {
        given:
        setupData()
        CellRangerMergingWorkPackage oldMWP = createMergingWorkPackage(status: CellRangerMergingWorkPackage.Status.DELETED, needsProcessing: false, sample: sampleA)
        CellRangerMwpParameter parameter = new CellRangerMwpParameter([
                seqType             : oldMWP.seqType,
                expectedCells       : oldMWP.expectedCells,
                enforcedCells       : oldMWP.enforcedCells,
                referenceGenomeIndex: oldMWP.referenceGenomeIndex,
        ])

        expect:
        cellRangerConfigurationService.findAllMergingWorkPackagesBySamplesAndPipeline(oldMWP.sample, parameter, requester)
    }

    @Unroll
    void "test createMergingWorkPackagesForSample creates MWP where #status MWP exists, fails"() {
        given:
        setupData()
        CellRangerMergingWorkPackage oldMWP = createMergingWorkPackage(status: status, needsProcessing: false, sample: sampleA)
        CellRangerMwpParameter parameter = new CellRangerMwpParameter([
                seqType             : oldMWP.seqType,
                expectedCells       : oldMWP.expectedCells,
                enforcedCells       : oldMWP.enforcedCells,
                referenceGenomeIndex: oldMWP.referenceGenomeIndex,
        ])

        when:
        cellRangerConfigurationService.findAllMergingWorkPackagesBySamplesAndPipeline(oldMWP.sample, parameter, requester)

        then:
        thrown(ValidationException)

        where:
        status                                    | _
        CellRangerMergingWorkPackage.Status.UNSET | _
        CellRangerMergingWorkPackage.Status.FINAL | _
    }

    void "test createMergingWorkPackagesForSample, creates an mwp for each LibPrepKit-SeqPlatformGroup combination of the SeqTracks"() {
        given:
        setupData()
        CellRangerMwpParameter parameter = createCellRangerMwpParameter()
        List<SeqTrack> seqTracks = setupMultipleSeqTracksOfDifferentSeqPlatformGroupsAndLibPrepKits()

        when:
        cellRangerConfigurationService.findAllMergingWorkPackagesBySamplesAndPipeline(sampleA, parameter, requester)

        then:
        seqTracks.size() == 8
        AssertionError e = thrown(AssertionError)
        e.message =~ "Can not handle SeqTracks processed over multiple platforms or with different library preparation kits."

        // TODO: when splitting of platforms and kits is implemented, this should be the expected behaviour:
        /*
        List<CellRangerMergingWorkPackage> all = CellRangerMergingWorkPackage.all
        all.size() == 6
        all*.sample.unique() == [sampleA]
        (seqTracks + seqTrackA) as Set == all*.seqTracks.flatten() as Set
        */
    }

    void "test createMergingWorkPackagesForSample, only considers SeqTracks of the given SeqType"() {
        given:
        setupData()
        CellRangerMwpParameter parameter = createCellRangerMwpParameter(seqType: seqType)

        SeqType otherSeqType = createSeqType(name: "otherSeqTypeA", dirName: "otherDir")
        Closure<SeqTrack> createSeqTrackHelper = { Map properties = [:] ->
            return createSeqTrack([
                    sample               : sampleA,
                    run                  : seqTrackA.run,
                    libraryPreparationKit: seqTrackA.libraryPreparationKit,
            ] + properties)
        }
        Set<SeqTrack> expected = [
                seqTrackA,
                createSeqTrackHelper(seqType: seqTrackA.seqType),
                createSeqTrackHelper(seqType: seqTrackA.seqType),
        ] as Set<SeqTrack>
        createSeqTrackHelper(seqType: otherSeqType)
        createSeqTrackHelper(seqType: otherSeqType)
        sampleA.refresh()

        when:
        cellRangerConfigurationService.findAllMergingWorkPackagesBySamplesAndPipeline(sampleA, parameter, requester)

        then:
        CellRangerMergingWorkPackage mwp = CollectionUtils.exactlyOneElement(CellRangerMergingWorkPackage.all)
        mwp.sample == sampleA
        CollectionUtils.containSame(mwp.seqTracks, expected)
    }

    void "test selectNoneAsFinal"() {
        given:
        createUserAndRoles()
        CellRangerMergingWorkPackage mwp1 = createMergingWorkPackage(expectedCells: 1)
        createBamFile([workPackage: mwp1])
        CellRangerMergingWorkPackage mwp2 = createMergingWorkPackage(expectedCells: 2, sample: mwp1.sample, seqType: mwp1.seqType,
                config: mwp1.config, referenceGenomeIndex: mwp1.referenceGenomeIndex)
        createBamFile([workPackage: mwp2])

        CellRangerConfigurationService cellRangerConfigurationService = new CellRangerConfigurationService()
        cellRangerConfigurationService.cellRangerWorkflowService = Mock(CellRangerWorkflowService) {
            0 * correctFilePermissions(_)
            1 * deleteOutputDirectory(mwp1.bamFileInProjectFolder)
            1 * deleteOutputDirectory(mwp2.bamFileInProjectFolder)
        }

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.selectNoneAsFinal(mwp1.sample, mwp1.seqType, mwp1.config.programVersion, mwp1.referenceGenomeIndex)
        }

        then:
        [mwp1, mwp2].every { it.status == CellRangerMergingWorkPackage.Status.DELETED }
    }

    void "test selectMwpAsFinal"() {
        given:
        createUserAndRoles()
        CellRangerMergingWorkPackage mwp1 = createMergingWorkPackage(expectedCells: 1)
        CellRangerMergingWorkPackage mwp2 = createMergingWorkPackage(expectedCells: 2, sample: mwp1.sample, seqType: mwp1.seqType, config: mwp1.config,
                referenceGenomeIndex: mwp1.referenceGenomeIndex)

        mwp1.bamFileInProjectFolder = createBamFile(workPackage: mwp1)
        mwp2.bamFileInProjectFolder = createBamFile(workPackage: mwp2)

        CellRangerConfigurationService cellRangerConfigurationService = new CellRangerConfigurationService()
        cellRangerConfigurationService.cellRangerWorkflowService = Mock(CellRangerWorkflowService) {
            1 * correctFilePermissions(mwp1.bamFileInProjectFolder)
            1 * deleteOutputDirectory(mwp2.bamFileInProjectFolder)
        }

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.selectMwpAsFinal(mwp1)
        }

        then:
        mwp1.status == CellRangerMergingWorkPackage.Status.FINAL
        mwp2.status == CellRangerMergingWorkPackage.Status.DELETED
    }

    void "test selectMwpAsFinal doesn't delete output dir for a rerun with same properties"() {
        given:
        createUserAndRoles()
        CellRangerMergingWorkPackage mwp1 = createMergingWorkPackage(expectedCells: 1, status: CellRangerMergingWorkPackage.Status.DELETED)
        CellRangerMergingWorkPackage mwp2 = createMergingWorkPackage(expectedCells: 1, sample: mwp1.sample, seqType: mwp1.seqType,
                libraryPreparationKit: mwp1.libraryPreparationKit, config: mwp1.config, referenceGenomeIndex: mwp1.referenceGenomeIndex)

        mwp1.bamFileInProjectFolder = createBamFile(workPackage: mwp1)
        mwp2.bamFileInProjectFolder = createBamFile(workPackage: mwp2)

        CellRangerConfigurationService cellRangerConfigurationService = new CellRangerConfigurationService()
        cellRangerConfigurationService.cellRangerWorkflowService = Mock(CellRangerWorkflowService) {
            0 * deleteOutputDirectory(mwp1.bamFileInProjectFolder)
            1 * correctFilePermissions(mwp2.bamFileInProjectFolder)
        }

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.selectMwpAsFinal(mwp2)
        }

        then:
        mwp2.status == CellRangerMergingWorkPackage.Status.FINAL
    }

    void "deleteMwps, sets status and calls delete"() {
        given:
        CellRangerConfigurationService cellRangerConfigurationService = new CellRangerConfigurationService(
                cellRangerWorkflowService: Mock(CellRangerWorkflowService)
        )
        List<CellRangerMergingWorkPackage> mwps = (1..3).collect {
            createBamFile().workPackage
        }
        mwps << createBamFile([fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS]).workPackage

        when:
        cellRangerConfigurationService.deleteMwps(mwps)

        then:
        mwps.size() * cellRangerConfigurationService.cellRangerWorkflowService.deleteOutputDirectory(_)
        mwps.every {
            it.status == CellRangerMergingWorkPackage.Status.DELETED
        }
    }

    void "deleteMwps, mwps contains final package, causes assertion error"() {
        given:
        CellRangerConfigurationService cellRangerConfigurationService = new CellRangerConfigurationService(
                cellRangerWorkflowService: Mock(CellRangerWorkflowService)
        )
        List<CellRangerMergingWorkPackage> mwps = (1..2).collect { createMergingWorkPackage() }
        mwps << createMergingWorkPackage(status: CellRangerMergingWorkPackage.Status.FINAL)

        when:
        cellRangerConfigurationService.deleteMwps(mwps)

        then:
        thrown(AssertionError)
    }

    void "setting an attribute should succeed"() {
        given:
        CellRangerConfigurationService crmwps = new CellRangerConfigurationService()

        CellRangerMergingWorkPackage crmwp = createMergingWorkPackage([
                informed: null,
        ])

        when: "setting value via service"
        crmwps.setInformedFlag(crmwp, Timestamp.valueOf(LocalDate.now().atStartOfDay()))

        then:
        crmwp.informed == Timestamp.valueOf(LocalDate.now().atStartOfDay())
    }
}
