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
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.monitor.MonitorOutputCollector
import de.dkfz.tbi.otp.ngsdata.*

@Rollback
@Integration
abstract class AbstractRoddyAlignmentCheckerIntegrationSpec extends Specification {

    AbstractRoddyAlignmentChecker checker

    List<SeqType> seqTypes


    void setup() {
        DomainFactory.createRoddyAlignableSeqTypes()
        checker = createRoddyAlignmentChecker()
        seqTypes = checker.seqTypes
    }


    abstract AbstractRoddyAlignmentChecker createRoddyAlignmentChecker()

    abstract Pipeline createPipeLine()

    //for checking with data of other PipeLine
    abstract Pipeline createPipeLineForCrosschecking()


    List<SeqTrack> createSeqTracks(Map properties = [:]) {
        seqTypes.collect {
            SeqTrack seqTrack = DomainFactory.createSeqTrack([
                    seqType              : it,
                    libraryPreparationKit: properties.hasProperty('libraryPreparationKit') ? properties.libraryPreparationKit :
                            it.isExome() ? DomainFactory.createLibraryPreparationKit() : null,
            ])
            DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
            return seqTrack
        }
    }

    List<SeqTrack> createSeqTracksWithConfig(Map configProperties = [:], Map seqTrackProperties = [:]) {
        createSeqTracks(seqTrackProperties).each {
            DomainFactory.createRoddyWorkflowConfig([
                    seqType: it.seqType,
                    project: it.project,
                    pipeline: createPipeLine(),
                    ] +  configProperties)
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


    RoddyBamFile createRoddyBamFile(MergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        DomainFactory.createRoddyBamFile([
                workPackage: mergingWorkPackage,
        ] + properties)
    }

    List<RoddyBamFile> createRoddyBamFiles(Map roddyProperties = [:], mergingWorkPackageProperties = [:]) {
        createMergingWorkPackages(mergingWorkPackageProperties).collect {
            createRoddyBamFile(it, roddyProperties)
        }
    }


    void "seqTracksWithoutCorrespondingRoddyAlignmentConfig, when some SamplePairs have a Config and some not some not, return Project and SeqType of SamplePairs without Config"() {
        given:
        List<SeqTrack> seqTracksWithoutConfig = createSeqTracks()

        List<SeqTrack> seqTracksCorrectConfig1 = createSeqTracksWithConfig()

        List<SeqTrack> seqTracksWrongProject = createSeqTracksWithConfig([
                project: DomainFactory.createProject(),
        ])

        List<SeqTrack> seqTracksWrongSeqType = createSeqTracksWithConfig([
                seqType: DomainFactory.createSeqType(),
        ])

        List<SeqTrack> seqTracksCorrectConfig2 = createSeqTracksWithConfig()

        List<SeqTrack> seqTracksAreObsolate = createSeqTracksWithConfig([
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
                seqTracksAreObsolate,
                seqTracksWrongPipeline,
        ].flatten()

        List<SeqTrack> expected = [
                seqTracksWithoutConfig,
                seqTracksWrongProject,
                seqTracksWrongSeqType,
                seqTracksAreObsolate,
                seqTracksWrongPipeline,
        ].flatten()

        when:
        List returnValue = checker.seqTracksWithoutCorrespondingRoddyAlignmentConfig(seqTracks)

        then:
        TestCase.assertContainSame(expected, returnValue)
    }


    void "mergingWorkPackageForSeqTracks, when for some SeqTracks a MergingWorkPackage exist and for some not, return list of SeqTracks without MergingWorkPackage and List of MergingWorkpackage"() {
        given:
        List<MergingWorkPackage> expectedMergingWorkPackage = []

        List<SeqTrack> seqTracksWrongSeqType = createSeqTracks()

        List<SeqTrack> seqTracksCorrect = createSeqTracks().each {
            expectedMergingWorkPackage << createMWP(it)
        }

        List<SeqTrack> seqTracksWrongSample = createSeqTracksWithMergingWorkPackages([
                sample: DomainFactory.createSample(),
        ])

        List<SeqTrack> seqTracksWrongSeqtype = createSeqTracksWithMergingWorkPackages([
                seqType: DomainFactory.createSeqType(),
        ])

        List<SeqTrack> seqTracksCorrectNoLibraryPreparationKit = createSeqTracks([
                libraryPreparationKit: null,
        ]).each {
            expectedMergingWorkPackage << createMWP(it)
        }

        List<SeqTrack> seqTrackWrongLibraryPreparationKit = createSeqTracks().each {
            if (!it.seqType.isWgbs()) {
                createMWP(it, [
                        libraryPreparationKit: DomainFactory.createLibraryPreparationKit(),
                ])
            }
        }.findAll()

        List<SeqTrack> seqTracksWrongPipeline = createSeqTracksWithMergingWorkPackages([
                pipeline: createPipeLineForCrosschecking(),
        ])

        List<SeqTrack> seqTracks = [
                seqTracksWrongSeqType,
                seqTracksCorrect,
                seqTracksWrongSample,
                seqTracksWrongSeqtype,
                seqTracksCorrectNoLibraryPreparationKit,
                seqTrackWrongLibraryPreparationKit,
                seqTracksWrongPipeline,
        ].flatten()

        List<SeqTrack> expectedSeqTracks = [
                seqTracksWrongSeqType,
                seqTracksWrongSample,
                seqTracksWrongSeqtype,
                seqTrackWrongLibraryPreparationKit,
                seqTracksWrongPipeline,
        ].flatten()

        when:
        Map returnValue = checker.mergingWorkPackageForSeqTracks(seqTracks)

        then:
        TestCase.assertContainSame(expectedSeqTracks, returnValue.seqTracksWithoutMergingWorkpackage)
        TestCase.assertContainSame(expectedMergingWorkPackage, returnValue.mergingWorkPackages)
    }


    @Unroll
    void "roddyBamFileForMergingWorkPackage, when some mergingWorkPackages have BamFiles and some not, return the correct BamFiles (case showFinished=#showFinished, showWithdrawn=#showWithdrawn)"() {
        given:
        int expectedSize = checker.seqTypes.size() * count
        List<RoddyBamFile> expected = []

        List<MergingWorkPackage> mergingWorkPackages = AbstractMergedBamFile.FileOperationStatus.values().collect { AbstractMergedBamFile.FileOperationStatus state ->
            [true, false].collect { boolean withDrawnRoddyBamFile ->
                createRoddyBamFiles([
                        fileOperationStatus: state,
                        withdrawn          : withDrawnRoddyBamFile,
                ]).collect { RoddyBamFile roddyBamFile ->
                    if ((showWithdrawn || !roddyBamFile.withdrawn) &&
                            (showFinished || roddyBamFile.fileOperationStatus != AbstractMergedBamFile.FileOperationStatus.PROCESSED)) {
                        expected << roddyBamFile
                    }
                    roddyBamFile.mergingWorkPackage
                }
            }
        }.flatten()

        when:
        List<RoddyBamFile> returnValue = checker.roddyBamFileForMergingWorkPackage(mergingWorkPackages, showFinished, showWithdrawn)

        then:
        expected.size() == expectedSize
        TestCase.assertContainSame(expected, returnValue)

        where:
        showFinished | showWithdrawn || count
        false        | false         || 3
        true         | false         || 4
        false        | true          || 6
        true         | true          || 8
    }


    void "handle, if SeqTracks given, then return finished RoddyBamFile and create output for the others cases"() {
        given:
        String workflowName = checker.workflowName
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        checker = Spy(checker.class)

        SeqTrack wrongSeqType = DomainFactory.createSeqTrack()

        List<SeqTrack> noConfigs = createSeqTracks()

        List<SeqTrack> noMergingWorkPackage = createSeqTracksWithConfig()

        List<RoddyBamFile> oldInstanceRunning = createRoddyBamFiles([
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
        List<RoddyBamFile> finishedRoddyBamFilesWithNeedsProcessing = createRoddyBamFiles([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
        ], [
                needsProcessing: true,
        ])
        waiting.addAll(finishedRoddyBamFilesWithNeedsProcessing*.mergingWorkPackage)
        mergingWorkPackageWaiting.addAll(finishedRoddyBamFilesWithNeedsProcessing*.containedSeqTracks.flatten())

        List<MergingWorkPackage> mergingWorkPackagesWithoutBam = []
        List<SeqTrack> mergingWorkPackageWithoutBam = createSeqTracksWithConfig().each {
            mergingWorkPackagesWithoutBam << createMWP(it, [
                    needsProcessing: false,
            ])
        }

        List<RoddyBamFile> roddyBamFilesDeclared = createRoddyBamFiles([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
        ], [
                needsProcessing: false,
        ])

        List<RoddyBamFile> roddyBamFilesNeedsProcessing = createRoddyBamFiles([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING,
        ], [
                needsProcessing: false,
        ])

        List<RoddyBamFile> roddyBamFilesInProgress = createRoddyBamFiles([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
        ], [
                needsProcessing: false,
        ])

        List<RoddyBamFile> roddyBamFilesProcessed = createRoddyBamFiles([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
        ], [
                needsProcessing: false,
        ])

        List<RoddyBamFile> roddyBamFilesWithdrawn = createRoddyBamFiles([
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
                        roddyBamFilesDeclared,
                        roddyBamFilesNeedsProcessing,
                        roddyBamFilesInProgress,
                        roddyBamFilesProcessed,
                        roddyBamFilesWithdrawn,
                ].flatten()*.containedSeqTracks,
        ].flatten()

        List<RoddyBamFile> finishedRoddyBamFiles = roddyBamFilesProcessed

        when:
        List<RoddyBamFile> result = checker.handle(seqTracks, output)

        then:
        1 * output.showWorkflow(workflowName)

        then:
        1 * output.showUniqueNotSupportedSeqTypes([wrongSeqType], _)

        then:
        1 * checker.seqTracksWithoutCorrespondingRoddyAlignmentConfig(_)
        1 * output.showUniqueList(AbstractRoddyAlignmentChecker.HEADER_NO_CONFIG, noConfigs, _)

        then:
        1 * checker.filter(_, _) >> { List<SeqTrack> seqTrackList, MonitorOutputCollector monitorOutputCollector ->
            return seqTrackList
        }

        then:
        1 * checker.mergingWorkPackageForSeqTracks(_)
        1 * output.showList(AbstractRoddyAlignmentChecker.HEADER_NO_MERGING_WORK_PACKAGE, noMergingWorkPackage)

        then:
        1 * checker.roddyBamFileForMergingWorkPackage(_, false, false)
        1 * output.showList(AbstractRoddyAlignmentChecker.HEADER_OLD_INSTANCE_RUNNING, oldInstanceRunning)

        then:
        1 * output.showWaiting(waiting)

        then:
        1 * checker.roddyBamFileForMergingWorkPackage(_, true, true)
        1 * output.showList(AbstractRoddyAlignmentChecker.HEADER_MWP_WITH_WITHDRAWN_BAM, roddyBamFilesWithdrawn*.mergingWorkPackage)
        1 * output.showList(AbstractRoddyAlignmentChecker.HEADER_MWP_WITHOUT_BAM, mergingWorkPackagesWithoutBam)

        then:
        1 * output.showRunningWithHeader(AbstractRoddyAlignmentChecker.HEADER_RUNNING_DECLARED, workflowName, roddyBamFilesDeclared + oldInstanceRunning)

        then:
        1 * output.showRunningWithHeader(AbstractRoddyAlignmentChecker.HEADER_RUNNING_NEEDS_PROCESSING, workflowName, roddyBamFilesNeedsProcessing)

        then:
        1 * output.showRunningWithHeader(AbstractRoddyAlignmentChecker.HEADER_RUNNING_IN_PROGRESS, workflowName, roddyBamFilesInProgress)

        then:
        1 * output.showFinished(roddyBamFilesProcessed)
        0 * output._

        then:
        finishedRoddyBamFiles == result
    }


    void "handle, if no SeqTracks given, then return empty list and do not create output"() {
        given:
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        checker = Spy(checker.class)

        when:
        List<RoddyBamFile> result = checker.handle([], output)

        then:
        0 * output._

        then:
        [] == result
    }
}
