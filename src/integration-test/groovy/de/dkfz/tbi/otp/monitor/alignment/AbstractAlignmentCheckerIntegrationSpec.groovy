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
package de.dkfz.tbi.otp.monitor.alignment

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.monitor.MonitorOutputCollector
import de.dkfz.tbi.otp.ngsdata.*

@Rollback
@Integration
abstract class AbstractAlignmentCheckerIntegrationSpec extends Specification {

    AbstractAlignmentChecker checker

    List<SeqType> seqTypes

    void setupData() {
        DomainFactory.createRoddyAlignableSeqTypes()
        DomainFactory.createCellRangerAlignableSeqTypes()
        checker = createAlignmentChecker()
        seqTypes = checker.seqTypes
    }

    abstract AbstractAlignmentChecker createAlignmentChecker()

    abstract Pipeline createPipeLine()

    //for checking with data of other PipeLine
    abstract Pipeline createPipeLineForCrosschecking()

    List<SeqTrack> createSeqTracks(Map properties = [:]) {
        seqTypes.collect {
            SeqTrack seqTrack = DomainFactory.createSeqTrack([
                    seqType              : it,
                    libraryPreparationKit: properties.hasProperty('libraryPreparationKit') ? properties.libraryPreparationKit :
                            it.needsBedFile ? DomainFactory.createLibraryPreparationKit() : null,
                    antibodyTarget       : it.hasAntibodyTarget ? DomainFactory.createAntibodyTarget() : null,
            ])
            DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
            return seqTrack
        }
    }

    List<SeqTrack> createSeqTracksWithConfig(Map configProperties = [:], Map seqTrackProperties = [:]) {
        createSeqTracks(seqTrackProperties).each {
            DomainFactory.createRoddyWorkflowConfig([
                    seqType : it.seqType,
                    project : it.project,
                    pipeline: createPipeLine(),
            ] + configProperties)
        }
    }

    List<SeqTrack> createSeqTracksWithMergingWorkPackages(Map mergingWorkPackageProperties = [:], Map seqTrackProperties = [:]) {
        createSeqTracks(seqTrackProperties).each {
            createMWP(it, mergingWorkPackageProperties)
        }
    }

    MergingWorkPackage createMWP(Map properties = [:]) {
        return DomainFactory.createMergingWorkPackage([
                pipeline: createPipeLine(),
                seqType : seqTypes.first(),
        ] + properties)
    }

    MergingWorkPackage createMWP(SeqTrack seqTrack, Map properties = [:]) {
        return createMWP([
                seqType              : seqTrack.seqType,
                sample               : seqTrack.sample,
                libraryPreparationKit: seqTrack.libraryPreparationKit,
                seqPlatformGroup     : seqTrack.seqPlatformGroup,
                seqTracks            : [seqTrack] as Set,
        ] + properties)
    }

    List<MergingWorkPackage> createMergingWorkPackages(Map properties = [:]) {
        return seqTypes.collect {
            createMWP([
                    seqType        : it,
                    needsProcessing: true,
            ] + properties)
        }
    }

    AbstractMergedBamFile createBamFile(MergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        DomainFactory.createRoddyBamFile([
                workPackage: mergingWorkPackage,
        ] + properties)
    }

    List<AbstractMergedBamFile> createBamFiles(Map bamProperties = [:], Map mergingWorkPackageProperties = [:]) {
        createMergingWorkPackages(mergingWorkPackageProperties).collect {
            createBamFile(it, bamProperties)
        }
    }

    void "test getSeqTracksWithoutCorrespondingAlignmentConfig, when some SamplePairs have a Config and some not some not, return Project and SeqType of SamplePairs without Config"() {
        given:
        setupData()
        List<SeqTrack> seqTracksWithoutConfig = createSeqTracks()

        List<SeqTrack> seqTracksCorrectConfig1 = createSeqTracksWithConfig()

        List<SeqTrack> seqTracksWrongProject = createSeqTracksWithConfig([
                project: DomainFactory.createProject(),
        ])

        List<SeqTrack> seqTracksWrongSeqType = createSeqTracksWithConfig([
                seqType: DomainFactory.createSeqType(),
        ])

        List<SeqTrack> seqTracksCorrectConfig2 = createSeqTracksWithConfig()

        List<SeqTrack> seqTracksAreObsolete = createSeqTracksWithConfig([
                obsoleteDate: new Date(),
        ])

        List<SeqTrack> seqTracksWrongPipeline = createSeqTracksWithConfig([
                pipeline: createPipeLineForCrosschecking(),
        ])

        List<SeqTrack> seqTracks = [
                seqTracksWithoutConfig,
                seqTracksCorrectConfig1,
                seqTracksWrongProject,
                seqTracksWrongSeqType,
                seqTracksCorrectConfig2,
                seqTracksAreObsolete,
                seqTracksWrongPipeline,
        ].flatten()

        List<SeqTrack> expected = [
                seqTracksWithoutConfig,
                seqTracksWrongProject,
                seqTracksWrongSeqType,
                seqTracksAreObsolete,
                seqTracksWrongPipeline,
        ].flatten()

        when:
        List returnValue = checker.getSeqTracksWithoutCorrespondingAlignmentConfig(seqTracks)

        then:
        TestCase.assertContainSame(expected, returnValue)
    }

    void "mergingWorkPackageForSeqTracks, when for some SeqTracks a MergingWorkPackage exist and for some not, return list of SeqTracks without MergingWorkPackage and List of MergingWorkpackage"() {
        given:
        setupData()
        List<MergingWorkPackage> expectedMergingWorkPackage = []

        List<SeqTrack> seqTracksOtherSeqType = createSeqTracks()

        List<SeqTrack> seqTracksCorrect = createSeqTracks().each {
            expectedMergingWorkPackage << createMWP(it)
        }

        List<SeqTrack> seqTracksWrongPipeline = createSeqTracksWithMergingWorkPackages([
                pipeline: createPipeLineForCrosschecking(),
        ])

        List<SeqTrack> seqTracksNoMergingWorkPackage = createSeqTracks()

        List<SeqTrack> seqTracks = [
                seqTracksOtherSeqType,
                seqTracksCorrect,
                seqTracksWrongPipeline,
                seqTracksNoMergingWorkPackage,
        ].flatten()

        List<SeqTrack> expectedSeqTracks = [
                seqTracksOtherSeqType,
                seqTracksWrongPipeline,
                seqTracksNoMergingWorkPackage,
        ].flatten()

        when:
        Map returnValue = checker.mergingWorkPackageForSeqTracks(seqTracks)

        then:
        TestCase.assertContainSame(expectedSeqTracks, returnValue.seqTracksWithoutMergingWorkpackage)
        TestCase.assertContainSame(expectedMergingWorkPackage, returnValue.mergingWorkPackages)
    }

    @Unroll
    void "test getBamFileForMergingWorkPackage, when some mergingWorkPackages have BamFiles and some not, return the correct BamFiles (case showFinished=#showFinished, showWithdrawn=#showWithdrawn)"() {
        given:
        setupData()
        List<AbstractMergedBamFile> expected = []

        List<MergingWorkPackage> mergingWorkPackages = AbstractMergedBamFile.FileOperationStatus.values().collectMany { AbstractMergedBamFile.FileOperationStatus state ->
            [true, false].collectMany { boolean withDrawnBamFile ->
                createBamFiles([
                        fileOperationStatus: state,
                        withdrawn          : withDrawnBamFile,
                ]).collect { AbstractMergedBamFile bamFile ->
                    if ((showWithdrawn || !bamFile.withdrawn) &&
                            (showFinished || bamFile.fileOperationStatus != AbstractMergedBamFile.FileOperationStatus.PROCESSED)) {
                        expected.add(bamFile)
                    }
                    bamFile.mergingWorkPackage
                }
            }
        }

        when:
        List<AbstractMergedBamFile> returnValue = checker.getBamFileForMergingWorkPackage(mergingWorkPackages, showFinished, showWithdrawn)

        then:
        TestCase.assertContainSame(expected, returnValue)

        where:
        showFinished | showWithdrawn || count
        false        | false         || 3
        true         | false         || 4
        false        | true          || 6
        true         | true          || 8
    }

    void "handle, if SeqTracks given, then return finished BamFile and create output for the others cases"() {
        given:
        setupData()
        String workflowName = checker.workflowName
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        checker = Spy(checker.class)

        SeqTrack wrongSeqType = DomainFactory.createSeqTrack()

        List<SeqTrack> noConfigs = createSeqTracks()

        List<SeqTrack> noMergingWorkPackage = createSeqTracksWithConfig()

        List<AbstractMergedBamFile> oldInstanceRunning = createBamFiles([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
        ], [
                needsProcessing: true,
        ])

        List<MergingWorkPackage> waiting = []
        List<SeqTrack> mergingWorkPackageWaiting = createSeqTracksWithConfig().each {
            waiting << createMWP(it, [
                    needsProcessing: true,
            ])
        }
        List<AbstractMergedBamFile> finishedBamFilesWithNeedsProcessing = createBamFiles([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
        ], [
                needsProcessing: true,
        ])
        waiting.addAll(finishedBamFilesWithNeedsProcessing*.mergingWorkPackage)
        mergingWorkPackageWaiting.addAll(finishedBamFilesWithNeedsProcessing*.containedSeqTracks.flatten())

        List<MergingWorkPackage> mergingWorkPackagesWithoutBam = []
        List<SeqTrack> mergingWorkPackageWithoutBam = createSeqTracksWithConfig().each {
            mergingWorkPackagesWithoutBam << createMWP(it, [
                    needsProcessing: false,
            ])
        }

        List<AbstractMergedBamFile> bamFilesDeclared = createBamFiles([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
        ], [
                needsProcessing: false,
        ])

        List<AbstractMergedBamFile> bamFilesNeedsProcessing = createBamFiles([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING,
        ], [
                needsProcessing: false,
        ])

        List<AbstractMergedBamFile> bamFilesInProgress = createBamFiles([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
        ], [
                needsProcessing: false,
        ])

        List<AbstractMergedBamFile> bamFilesProcessed = createBamFiles([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
        ], [
                needsProcessing: false,
        ])

        List<AbstractMergedBamFile> bamFilesWithdrawn = createBamFiles([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
                withdrawn          : true,
        ], [
                needsProcessing: false,
        ])

        List<SeqTrack> seqTracks = [
                wrongSeqType,
                noConfigs,
                noMergingWorkPackage,
                mergingWorkPackageWaiting,
                mergingWorkPackageWithoutBam,
                [
                        oldInstanceRunning,
                        bamFilesDeclared,
                        bamFilesNeedsProcessing,
                        bamFilesInProgress,
                        bamFilesProcessed,
                        bamFilesWithdrawn,
                ].flatten()*.containedSeqTracks,
        ].flatten()

        seqTracks.each {
            DomainFactory.createReferenceGenomeProjectSeqTypeLazy(project: it.project, seqType: it.seqType)
        }

        List<AbstractMergedBamFile> finishedBamFiles = bamFilesProcessed

        when:
        List<AbstractMergedBamFile> result = checker.handle(seqTracks, output)

        then:
        1 * output.showWorkflowOldSystem(workflowName)

        then:
        1 * output.showUniqueNotSupportedSeqTypes([wrongSeqType], _)

        then:
        1 * checker.getSeqTracksWithoutCorrespondingAlignmentConfig(_)
        1 * output.showUniqueList(AbstractAlignmentChecker.HEADER_NO_CONFIG, noConfigs, _)

        then:
        1 * checker.filter(_, _) >> { List<SeqTrack> seqTrackList, MonitorOutputCollector monitorOutputCollector ->
            return seqTrackList
        }

        then:
        1 * checker.mergingWorkPackageForSeqTracks(_)
        1 * output.showList(AbstractAlignmentChecker.HEADER_NO_MERGING_WORK_PACKAGE, _) >> { String name, List objects ->
            TestCase.assertContainSame(objects, noMergingWorkPackage)
        }

        then:
        1 * checker.getBamFileForMergingWorkPackage(_, false, false)
        1 * output.showList(AbstractAlignmentChecker.HEADER_OLD_INSTANCE_RUNNING, _) >> { String name, List objects ->
            TestCase.assertContainSame(objects, oldInstanceRunning)
        }

        then:
        1 * output.showWaiting(_) >> { List objects ->
            TestCase.assertContainSame(objects[0], waiting)
        }

        then:
        1 * checker.getBamFileForMergingWorkPackage(_, true, true)
        1 * output.showList(AbstractAlignmentChecker.HEADER_MWP_WITH_WITHDRAWN_BAM, _) >> { String name, List objects ->
            TestCase.assertContainSame(objects, bamFilesWithdrawn*.mergingWorkPackage)
        }
        1 * output.showList(AbstractAlignmentChecker.HEADER_MWP_WITHOUT_BAM, _) >> { String name, List objects ->
            TestCase.assertContainSame(objects, mergingWorkPackagesWithoutBam)
        }

        then:
        1 * output.showRunningWithHeader(AbstractAlignmentChecker.HEADER_RUNNING_DECLARED, workflowName, _) >> { String header, String workflowp, List objects ->
            TestCase.assertContainSame(objects, bamFilesDeclared + oldInstanceRunning)
        }

        then:
        1 * output.showRunningWithHeader(AbstractAlignmentChecker.HEADER_RUNNING_NEEDS_PROCESSING, workflowName, _) >> { String header, String workflowp, List objects ->
            TestCase.assertContainSame(objects, bamFilesNeedsProcessing)
        }

        then:
        1 * output.showRunningWithHeader(AbstractAlignmentChecker.HEADER_RUNNING_IN_PROGRESS, workflowName, _) >> { String header, String workflowp, List objects ->
            TestCase.assertContainSame(objects, bamFilesInProgress)
        }

        then:
        1 * output.showFinished(_) >> { List objects ->
            TestCase.assertContainSame(objects[0], bamFilesProcessed)
        }
        0 * output._

        then:
        finishedBamFiles == result
    }

    void "handle, if no SeqTracks given, then return empty list and do not create output"() {
        given:
        setupData()
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        checker = Spy(checker.class)

        when:
        List<AbstractMergedBamFile> result = checker.handle([], output)

        then:
        0 * output._

        then:
        [] == result
    }
}
