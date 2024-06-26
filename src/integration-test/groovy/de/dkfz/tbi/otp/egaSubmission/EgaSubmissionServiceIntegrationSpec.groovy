/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.egaSubmission

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.submissions.ega.EgaSubmissionFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
class EgaSubmissionServiceIntegrationSpec extends Specification implements EgaSubmissionFactory, IsRoddy {

    private final EgaSubmissionService egaSubmissionService = new EgaSubmissionService()

    EgaSubmission egaSubmission1
    EgaSubmission egaSubmission2

    @Unroll
    void "checkFastqFiles, when withRawSequenceFile is #withRawSequenceFile and fileExists is #fileExists and withdrawn is #withdrawn, then result is #result"() {
        given:
        SeqTrack seqTrack = createSeqTrack()
        if (withRawSequenceFile) {
            createFastqFile([
                    seqTrack     : seqTrack,
                    fileWithdrawn: withdrawn,
                    fileExists   : fileExists,
            ])
        } else {
            createFastqFile()
        }

        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
        )

        EgaSubmission submission = createEgaSubmission(
                project: seqTrack.project
        )
        submission.addToSamplesToSubmit(sampleSubmissionObject)
        submission.save(flush: true)

        when:
        Map map = egaSubmissionService.checkFastqFiles(submission)

        then:
        map.get(sampleSubmissionObject) == result

        where:
        withRawSequenceFile | fileExists | withdrawn || result
        true                | true       | false     || true
        false               | true       | false     || false
        true                | true       | true      || true
        true                | false      | false     || false
        true                | false      | true      || false
    }

    @Unroll
    void "checkBamFiles, when withBamFile is #withBamFile and withdrawn is #withdrawn and status is #status, then result is #result"() {
        given:
        RoddyBamFile roddyBamFile = createBamFile(
                withdrawn: withdrawn,
                fileOperationStatus: status,
        )
        if (bamFileInProjectFolderSet) {
            roddyBamFile.workPackage.bamFileInProjectFolder = roddyBamFile
            roddyBamFile.workPackage.save(flush: true)
        }

        SampleSubmissionObject sampleSubmissionObject
        if (withBamFile) {
            sampleSubmissionObject = createSampleSubmissionObject(
                    sample: roddyBamFile.sample,
                    seqType: roddyBamFile.seqType,
            )
        } else {
            sampleSubmissionObject = createSampleSubmissionObject()
        }

        EgaSubmission submission = createEgaSubmission(
                project: sampleSubmissionObject.project
        )
        submission.addToSamplesToSubmit(sampleSubmissionObject)
        submission.save(flush: true)

        when:
        Map map = egaSubmissionService.checkBamFiles(submission)

        then:
        map.get(sampleSubmissionObject) == result

        where:
        withBamFile | withdrawn | bamFileInProjectFolderSet | status                                         || result
        true        | false     | true                      | AbstractBamFile.FileOperationStatus.PROCESSED  || true
        false       | false     | true                      | AbstractBamFile.FileOperationStatus.PROCESSED  || false
        true        | true      | true                      | AbstractBamFile.FileOperationStatus.PROCESSED  || true
        true        | false     | true                      | AbstractBamFile.FileOperationStatus.INPROGRESS || false
        true        | false     | false                     | AbstractBamFile.FileOperationStatus.PROCESSED  || false
    }

    @Unroll
    void "egaUploadIsInProgress returns a list of EgaSubmission with specified state"() {
        given:
        Project project = createProject()
        SeqTrack seqTrack = createSeqTrackWithOneFastqFile()

        egaSubmission1 = createEgaSubmission([
                project                 : project,
                state                   : ega1UploadState,
                samplesToSubmit         : [
                        createSampleSubmissionObject([
                                sample      : seqTrack.sample,
                                seqType     : seqTrack.seqType,
                                useFastqFile: true,
                        ]),
                ] as Set,
                rawSequenceFilesToSubmit: [
                        createRawSequenceFileSubmissionObject(),
                ] as Set,
        ])
        egaSubmission2 = createEgaSubmission([
                project                 : project,
                state                   : ega2UploadState,
                samplesToSubmit         : [
                        createSampleSubmissionObject([
                                sample      : seqTrack.sample,
                                seqType     : seqTrack.seqType,
                                useFastqFile: false,
                        ]),
                ] as Set,
                rawSequenceFilesToSubmit: [
                        createRawSequenceFileSubmissionObject(),
                ] as Set,
        ])

        when:
        boolean result = egaSubmissionService.egaUploadIsInProgress(project)

        then:
        result == expectedFlag

        where:
        name            | ega1UploadState                          | ega2UploadState                          || expectedFlag
        'All finished'  | EgaSubmission.State.FILE_UPLOAD_FINISHED | EgaSubmission.State.FILE_UPLOAD_FINISHED || false
        'One uploading' | EgaSubmission.State.FILE_UPLOAD_STARTED  | EgaSubmission.State.FILE_UPLOAD_FINISHED || true
        'All uploading' | EgaSubmission.State.FILE_UPLOAD_STARTED  | EgaSubmission.State.FILE_UPLOAD_STARTED  || true
    }

    void "getRawSequenceFilesAndAlias, when already DataFiles are connected with the submission, then return these"() {
        given:
        SeqTrack seqTrack1 = createSeqTrackWithOneFastqFile()
        SeqTrack seqTrack2 = createSeqTrackWithOneFastqFile([
                sample : seqTrack1.sample,
                seqType: seqTrack1.seqType,
        ])
        RawSequenceFile rawSequenceFile = CollectionUtils.exactlyOneElement(seqTrack2.sequenceFiles)
        EgaSubmission egaSubmission = createEgaSubmission([
                samplesToSubmit  : [
                        createSampleSubmissionObject([
                                sample      : seqTrack1.sample,
                                seqType     : seqTrack1.seqType,
                                useFastqFile: true,
                        ]),
                ] as Set,
                rawSequenceFilesToSubmit: [
                        createRawSequenceFileSubmissionObject([
                                sequenceFile: rawSequenceFile,
                        ]),
                ] as Set,
        ])
        egaSubmissionService.seqTrackService = new SeqTrackService([
                fileTypeService: new FileTypeService(),
        ])

        when:
        List<RawSequenceFileAndSampleAlias> list = egaSubmissionService.getRawSequenceFilesAndAlias(egaSubmission)

        then:
        list*.rawSequenceFile == [rawSequenceFile]
    }

    void "getRawSequenceFilesAndAlias, when sample has two lane, then return both"() {
        given:
        SeqTrack seqTrack1 = createSeqTrackWithOneFastqFile()
        SeqTrack seqTrack2 = createSeqTrackWithOneFastqFile([
                sample : seqTrack1.sample,
                seqType: seqTrack1.seqType,
        ])
        EgaSubmission egaSubmission = createEgaSubmission([
                samplesToSubmit: [
                        createSampleSubmissionObject([
                                sample      : seqTrack1.sample,
                                seqType     : seqTrack1.seqType,
                                useFastqFile: true,
                        ]),
                ] as Set,
        ])
        egaSubmissionService.seqTrackService = new SeqTrackService([
                fileTypeService: new FileTypeService(),
        ])

        List<RawSequenceFile> expectedRawSequenceFiles = [
                seqTrack1,
                seqTrack2,
        ]*.sequenceFiles.flatten()

        when:
        List<RawSequenceFileAndSampleAlias> list = egaSubmissionService.getRawSequenceFilesAndAlias(egaSubmission)

        then:
        TestCase.assertContainSame(list*.rawSequenceFile, expectedRawSequenceFiles)
    }

    @Unroll
    void "getRawSequenceFilesAndAlias, when case '#name', then return datafile: #returnedValue"() {
        given:
        SeqTrack seqTrack = createSeqTrack()
        RawSequenceFile rawSequenceFile
        if (fileExists) {
            rawSequenceFile = createFastqFile([
                    seqTrack     : seqTrack,
                    fileWithdrawn: withdrawn,
                    fileExists   : fileExistOnFileSystem,
                    fileType     : createFileType([
                            type: fileType,
                    ]),
            ])
        }
        EgaSubmission egaSubmission = createEgaSubmission([
                samplesToSubmit: [
                        createSampleSubmissionObject([
                                sample      : seqTrack.sample,
                                seqType     : seqTrack.seqType,
                                useFastqFile: useFastqFile,
                        ]),
                ] as Set,
        ])
        egaSubmissionService.seqTrackService = new SeqTrackService([
                fileTypeService: new FileTypeService(),
        ])

        List<RawSequenceFile> expectedRawSequenceFiles = returnedValue ? [rawSequenceFile,] : []

        when:
        List<RawSequenceFileAndSampleAlias> list = egaSubmissionService.getRawSequenceFilesAndAlias(egaSubmission)

        then:
        TestCase.assertContainSame(list*.rawSequenceFile, expectedRawSequenceFiles)

        where:
        name                               | fileExists    | useFastqFile | withdrawn | fileExistOnFileSystem | fileType               || returnedValue
        'all fine'                         | true          | true         | false     | true                  | FileType.Type.SEQUENCE || true
        'no data file'                     | false         | true         | false     | true                  | FileType.Type.SEQUENCE || false
        'useFastq is false'                | true          | false        | false     | true                  | FileType.Type.SEQUENCE || false
        'withdrawn'                        | true          | true         | true      | true                  | FileType.Type.SEQUENCE || true
        'file do not exist on file system' | true          | true         | false     | false                 | FileType.Type.SEQUENCE || false
        'fie type is not sequence'         | true          | true         | false     | true                  | FileType.Type.UNKNOWN  || false
    }

    void "getBamFilesAndAlias, when already BamFiles are connected with the submission, then return these"() {
        given:
        EgaSubmission submission = createEgaSubmission()
        RoddyBamFile bamFile = createBamFile()
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject(
                sample: bamFile.sample,
                seqType: bamFile.seqType,
        )
        submission.addToSamplesToSubmit(sampleSubmissionObject)
        BamFileSubmissionObject bamFileSubmissionObject = createBamFileSubmissionObject(
                bamFile: bamFile,
                sampleSubmissionObject: sampleSubmissionObject,
        )
        submission.addToBamFilesToSubmit(bamFileSubmissionObject)

        when:
        List bamFilesAndAlias = egaSubmissionService.getBamFilesAndAlias(submission)

        then:
        bamFilesAndAlias*.bamFile == [bamFile]
        bamFilesAndAlias*.sampleSubmissionObject == [sampleSubmissionObject]
    }

    @Unroll
    void "getBamFilesAndAlias, when case '#name', then return bamFile: #returnedValue"() {
        given:
        RoddyBamFile roddyBamFile = createBamFile(
                withdrawn: withdrawn,
                fileOperationStatus: fileOperationStatus,
        )
        if (bamFileInProjectFolderSet) {
            roddyBamFile.workPackage.bamFileInProjectFolder = roddyBamFile
            roddyBamFile.workPackage.save(flush: true)
        }

        SampleSubmissionObject sampleSubmissionObject
        if (bamFileExist) {
            sampleSubmissionObject = createSampleSubmissionObject([
                    sample    : roddyBamFile.sample,
                    seqType   : roddyBamFile.seqType,
                    useBamFile: useBamFile,
            ])
        } else {
            sampleSubmissionObject = createSampleSubmissionObject()
        }

        EgaSubmission egaSubmission = createEgaSubmission([
                project        : sampleSubmissionObject.project,
                samplesToSubmit: [
                        sampleSubmissionObject,
                ].toSet(),
        ])

        List<AbstractBamFile> expectedBamFiles = returnedValue ? [roddyBamFile,] : []

        when:
        List<BamFileAndSampleAlias> list = egaSubmissionService.getBamFilesAndAlias(egaSubmission)

        then:
        TestCase.assertContainSame(list*.bamFile, expectedBamFiles)

        where:
        name                                   | bamFileExist | useBamFile | withdrawn | bamFileInProjectFolderSet | fileOperationStatus                            || returnedValue
        'all fine'                             | true         | true       | false     | true                      | AbstractBamFile.FileOperationStatus.PROCESSED  || true
        'no bam file'                          | false        | true       | false     | true                      | AbstractBamFile.FileOperationStatus.PROCESSED  || false
        'useBamFile is false'                  | true         | false      | false     | true                      | AbstractBamFile.FileOperationStatus.PROCESSED  || false
        'bam file is withdrawn'                | true         | true       | true      | true                      | AbstractBamFile.FileOperationStatus.PROCESSED  || true
        'bamFileInProjectFolder is not set'    | true         | true       | false     | false                     | AbstractBamFile.FileOperationStatus.PROCESSED  || false
        'fileOperationStatus is not PROCESSED' | true         | true       | false     | true                      | AbstractBamFile.FileOperationStatus.INPROGRESS || false
    }

    void "test create bam file submission objects"() {
        given:
        EgaSubmission submission = createEgaSubmission()
        RoddyBamFile bamFile = createBamFile()
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject(
                sample: bamFile.sample,
                seqType: bamFile.seqType,
                useBamFile: true,
        )
        submission.addToSamplesToSubmit(sampleSubmissionObject)

        when:
        egaSubmissionService.createBamFileSubmissionObjects(submission)

        then:
        submission.bamFilesToSubmit.size() == BamFileSubmissionObject.findAll().size()
    }

    void "getExperimentalMetadata, when a datafile is given, then return one experiment of this datafile"() {
        given:
        RawSequenceFileSubmissionObject submissionObject = createRawSequenceFileSubmissionObject()
        RawSequenceFile rawSequenceFile = submissionObject.sequenceFile

        EgaSubmission submission = createEgaSubmission(
                rawSequenceFilesToSubmit: [submissionObject],
        )

        when:
        List<Map> experimentalMetadata = egaSubmissionService.getExperimentalMetadata(submission)

        then:
        experimentalMetadata.size() == 1
        Map metadata = experimentalMetadata[0] as Map
        metadata['libraryLayout'] == rawSequenceFile.seqType.libraryLayout
        metadata['displayName'] == rawSequenceFile.seqType.displayName
        metadata['libraryPreparationKit'] == rawSequenceFile.seqTrack.libraryPreparationKit
        metadata['mappedEgaPlatformModel']
        metadata['mappedEgaLibrarySource']
        metadata['mappedEgaLibraryStrategy']
        metadata['mappedEgaLibrarySelection']
    }

    void "getExperimentalMetadata, when a bam file with one seqplatform and one library preparation kit, then return one experiment of this bam file"() {
        given:
        BamFileSubmissionObject bamFileSubmissionObject = createBamFileSubmissionObject()
        AbstractBamFile bamFile = bamFileSubmissionObject.bamFile

        EgaSubmission submission = createEgaSubmission(
                bamFilesToSubmit: [bamFileSubmissionObject],
        )

        when:
        List<Map> experimentalMetadata = egaSubmissionService.getExperimentalMetadata(submission)

        then:
        experimentalMetadata.size() == 1
        Map metadata = experimentalMetadata[0] as Map
        metadata['libraryLayout'] == bamFile.seqType.libraryLayout
        metadata['displayName'] == bamFile.seqType.displayName
        metadata['libraryPreparationKit'] == bamFile.mergingWorkPackage.libraryPreparationKit
        metadata['mappedEgaPlatformModel']
        metadata['mappedEgaLibrarySource']
        metadata['mappedEgaLibraryStrategy']
        metadata['mappedEgaLibrarySelection']
    }

    void "getExperimentalMetadata, when a bam file with three seqPlatform and three library preparation kits, then return three experiments of this bam file"() {
        given:
        BamFileSubmissionObject bamFileSubmissionObject = createBamFileSubmissionObject()
        RoddyBamFile bamFile = bamFileSubmissionObject.bamFile
        SeqTrack seqTrack2 = createSeqTrack()
        SeqTrack seqTrack3 = createSeqTrack()
        bamFile.addToSeqTracks(seqTrack2)
        bamFile.addToSeqTracks(seqTrack3)
        bamFile.numberOfMergedLanes = 3
        bamFile.save(flush: true)

        EgaSubmission submission = createEgaSubmission(
                bamFilesToSubmit: [bamFileSubmissionObject],
        )

        when:
        List<Map> experimentalMetadata = egaSubmissionService.getExperimentalMetadata(submission)

        then:
        experimentalMetadata.size() == 3
    }

    void "getExperimentalMetadata, when a patient has multiple data, then return only the experiment of the data in the submission"() {
        given:
        RawSequenceFileSubmissionObject submissionObject = createRawSequenceFileSubmissionObject()
        RawSequenceFile rawSequenceFile = submissionObject.sequenceFile
        createFastqFile([
                seqTrack: createSeqTrack([
                        sample: rawSequenceFile.sample,
                ])
        ])

        EgaSubmission submission = createEgaSubmission(
                rawSequenceFilesToSubmit: [submissionObject],
        )

        when:
        List<Map> experimentalMetadata = egaSubmissionService.getExperimentalMetadata(submission)

        then:
        experimentalMetadata.size() == 1
    }
}
