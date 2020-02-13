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
import org.springframework.validation.Errors
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils

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
    ReferenceGenomeIndex referenceGenomeIndex

    void setupData() {
        createUserAndRoles()

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
        createSeqTrack(seqType: seqType, sample: sampleB)
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
        samples.allSamples == [sampleA, sampleB]
        samples.selectedSamples == [sampleA]
    }

    void "test getSamples for whole project"() {
        given:
        setupData()

        when:
        CellRangerConfigurationService.Samples samples = SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.getSamples(project, null, null)
        }

        then:
        samples.allSamples == [sampleA, sampleB]
        samples.selectedSamples == [sampleA, sampleB]
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

    @Unroll
    void "test createMergingWorkPackage (expectedCells=#expectedCells, enforcedCells=#enforcedCells)"() {
        given:
        setupData()

        when:
        Errors errors = SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.createMergingWorkPackage(expectedCells, enforcedCells, referenceGenomeIndex, project, individual, sampleType, seqType)
        }

        then:
        !errors
        CellRangerMergingWorkPackage mwp = CollectionUtils.exactlyOneElement(CellRangerMergingWorkPackage.all)
        mwp.sample == sampleA
        mwp.seqTracks == [seqTrackA] as Set
        mwp.expectedCells == expectedCells
        mwp.enforcedCells == enforcedCells
        mwp.referenceGenomeIndex == referenceGenomeIndex
        mwp.project == project
        mwp.seqType == seqType
        mwp.individual == individual

        where:
        expectedCells | enforcedCells
        5000          | null
        null          | 5000
    }

    void "test createMergingWorkPackage for whole project"() {
        given:
        setupData()

        when:
        Errors errors = SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.createMergingWorkPackage(1, null, referenceGenomeIndex, project, null, null, seqType)
        }

        then:
        !errors
        List<CellRangerMergingWorkPackage> all = CellRangerMergingWorkPackage.all
        all.size() == 2
        all*.sample as Set == [sampleA, sampleB] as Set
    }

    void "test createMergingWorkPackage, creates an mwp for each LibPrepKit-SeqPlatformGroup combination of the SeqTracks"() {
        given:
        setupData()
        List<SeqTrack> seqTracks = setupMultipleSeqTracksOfDifferentSeqPlatformGroupsAndLibPrepKits()

        when:
        Errors errors = SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.createMergingWorkPackage(1, null, referenceGenomeIndex, project, sampleA.individual, sampleA.sampleType, seqType)
        }

        then:
        !errors
        seqTracks.size() == 8
        AssertionError e = thrown(AssertionError)
        e.message =~ "Can not handle SeqTracks processed over multiple platforms or with different library preparation kits."

        // TODO: when splitting of platforms and kits is implemented, this should be the expected behaviour:
        /*
        !errors
        List<CellRangerMergingWorkPackage> all = CellRangerMergingWorkPackage.all
        all.size() == 6
        all*.sample.unique() == [sampleA]
        (seqTracks + seqTrackA) as Set == all*.seqTracks.flatten() as Set
        */
    }

    void "test createMergingWorkPackage, only considers SeqTracks of the given SeqType"() {
        given:
        setupData()

        createSeqTrack(sample: sampleA)
        createSeqTrack(sample: sampleA)
        createSeqTrack(sample: sampleA)

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.createMergingWorkPackage(1, null, referenceGenomeIndex, project, sampleA.individual, sampleA.sampleType, seqType)
        }

        then:
        List<CellRangerMergingWorkPackage> all = CellRangerMergingWorkPackage.all
        all.size() == 1
        all*.sample as Set == [sampleA] as Set
    }

    void "test selectNoneAsFinal"() {
        given:
        createUserAndRoles()
        CellRangerMergingWorkPackage mwp1 = createMergingWorkPackage(expectedCells: 1)
        CellRangerMergingWorkPackage mwp2 = createMergingWorkPackage(expectedCells: 2, sample: mwp1.sample, seqType: mwp1.seqType, config: mwp1.config, referenceGenomeIndex: mwp1.referenceGenomeIndex)

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
        CellRangerMergingWorkPackage mwp2 = createMergingWorkPackage(expectedCells: 2, sample: mwp1.sample, seqType: mwp1.seqType, config: mwp1.config, referenceGenomeIndex: mwp1.referenceGenomeIndex)

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
}
