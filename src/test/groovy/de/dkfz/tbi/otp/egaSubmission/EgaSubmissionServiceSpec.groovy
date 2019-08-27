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
package de.dkfz.tbi.otp.egaSubmission

import grails.testing.gorm.DataTest
import grails.validation.ValidationException
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.submissions.ega.EgaSubmissionFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

class EgaSubmissionServiceSpec extends Specification implements EgaSubmissionFactory, IsRoddy, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                AbstractMergedBamFile,
                BamFileSubmissionObject,
                DataFile,
                DataFileSubmissionObject,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingWorkPackage,
                Pipeline,
                Project,
                Realm,
                ReferenceGenome,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Run,
                RunSegment,
                Sample,
                SampleSubmissionObject,
                SampleType,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
                EgaSubmission,
        ]
    }

    private final EgaSubmissionService egaSubmissionService = new EgaSubmissionService()

    @SuppressWarnings('UnnecessaryObjectReferences')
    @Unroll
    void "test create submission all valid"() {
        given:
        Map params = [
                project       : createProject(),
                egaBox        : egaBox,
                submissionName: submissionName,
                studyName     : studyName,
                studyType     : EgaSubmission.StudyType.CANCER_GENOMICS,
                studyAbstract : studyAbstract,
                pubMedId      : pubMedId,
        ]

        when:
        egaSubmissionService.createSubmission(params)
        EgaSubmission submission = CollectionUtils.exactlyOneElement(EgaSubmission.list())

        then:
        submission.egaBox == egaBox
        submission.submissionName == submissionName
        submission.studyType == studyType
        submission.studyAbstract == studyAbstract
        submission.pubMedId == pubMedId
        submission.state == EgaSubmission.State.SELECTION

        where:
        egaBox   | submissionName   | studyName   | studyType                               | studyAbstract   | pubMedId
        "egaBox" | "submissionName" | "studyName" | EgaSubmission.StudyType.CANCER_GENOMICS | "studyAbstract" | "pubMedId"
        "egaBox" | "submissionName" | "studyName" | EgaSubmission.StudyType.CANCER_GENOMICS | "studyAbstract" | null
    }

    @Unroll
    void "test create submission with exception #exceptionMessage"() {
        given:
        Map params = [
                project       : createProject(),
                egaBox        : egaBox,
                submissionName: submissionName,
                studyName     : studyName,
                studyType     : studyType,
                studyAbstract : studyAbstract,
        ]

        when:
        egaSubmissionService.createSubmission(params)

        then:
        ValidationException exception = thrown()
        exception.message.contains(exceptionMessage)

        where:
        egaBox   | submissionName   | studyName   | studyType                               | studyAbstract   || exceptionMessage
        null     | "submissionName" | "studyName" | EgaSubmission.StudyType.CANCER_GENOMICS | "studyAbstract" || "'egaBox': rejected value [null]"
        "egaBox" | null             | "studyName" | EgaSubmission.StudyType.CANCER_GENOMICS | "studyAbstract" || "'submissionName': rejected value [null]"
        "egaBox" | "submissionName" | null        | EgaSubmission.StudyType.CANCER_GENOMICS | "studyAbstract" || "'studyName': rejected value [null]"
        "egaBox" | "submissionName" | "studyName" | null                                    | "studyAbstract" || "'studyType': rejected value [null]"
        "egaBox" | "submissionName" | "studyName" | EgaSubmission.StudyType.CANCER_GENOMICS | null            || "'studyAbstract': rejected value [null]"
    }

    void "test update state all fine"() {
        given:
        EgaSubmission submission = createEgaSubmission()
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject()
        BamFileSubmissionObject bamFileSubmissionObject = createBamFileSubmissionObject(
                sampleSubmissionObject: sampleSubmissionObject,
        )
        DataFileSubmissionObject dataFileSubmissionObject = createDataFileSubmissionObject(
                sampleSubmissionObject: sampleSubmissionObject,
        )
        submission.addToBamFilesToSubmit(bamFileSubmissionObject)
        submission.addToDataFilesToSubmit(dataFileSubmissionObject)
        submission.addToSamplesToSubmit(sampleSubmissionObject)
        submission.save(flush: true)

        when:
        egaSubmissionService.updateSubmissionState(submission, EgaSubmission.State.FILE_UPLOAD_STARTED)

        then:
        EgaSubmission.State.FILE_UPLOAD_STARTED == submission.state
    }

    void "test update state without files"() {
        given:
        EgaSubmission submission = createEgaSubmission()

        when:
        egaSubmissionService.updateSubmissionState(submission, EgaSubmission.State.FILE_UPLOAD_STARTED)

        then:
        ValidationException exception = thrown()
        exception.message.contains("rejected value [[]]")
    }

    void "test save submission object all fine"() {
        given:
        EgaSubmission submission = createEgaSubmission()
        Sample sample = createSample()
        SeqType seqType = createSeqType()

        when:
        egaSubmissionService.saveSampleSubmissionObject(submission, sample, seqType)

        then:
        !submission.samplesToSubmit.isEmpty()
        submission.samplesToSubmit.first().seqType == seqType
        submission.samplesToSubmit.first().sample == sample
    }

    @Unroll
    void "test save submission object"() {
        given:
        EgaSubmission submission = createEgaSubmission()

        when:
        egaSubmissionService.saveSampleSubmissionObject(submission, sample(), seqType())

        then:
        ValidationException exception = thrown()
        exception.message.contains("rejected value [null]")

        where:
        sample                 | seqType
        ( { null } )           | { createSeqType() }
        ( { createSample() } ) | { null }
    }

    void "test update submission object all fine"() {
        given:
        EgaSubmission submission = createEgaSubmission()
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject()

        when:
        egaSubmissionService.updateSampleSubmissionObjects(submission, ["${sampleSubmissionObject.id}"], ["newAlias"], [EgaSubmissionService.FileType.BAM])

        then:
        sampleSubmissionObject.egaAliasName == "newAlias"
        sampleSubmissionObject.useBamFile
        !sampleSubmissionObject.useFastqFile
    }

    void "test remove sample submission objects"() {
        given:
        EgaSubmission submission = createEgaSubmission()
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject()
        submission.addToSamplesToSubmit(sampleSubmissionObject)

        when:
        List l = egaSubmissionService.deleteSampleSubmissionObjects(submission)

        then:
        submission.samplesToSubmit.isEmpty()
        l == ["${sampleSubmissionObject.sample.id}${sampleSubmissionObject.seqType.toString()}"]
    }

    @Unroll
    void "test check file types with data file"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        if (withDataFile) {
            DomainFactory.createDataFile(
                    seqTrack: seqTrack,
                    fileWithdrawn: withdrawn,
            )
        } else {
            DomainFactory.createDataFile()
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
        withDataFile | withdrawn | result
        true         | false     | true
        false        | false     | false
        true         | true      | true
    }

    void "test get bam files and alias"() {
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
        bamFilesAndAlias*.sampleAlias == [sampleSubmissionObject.egaAliasName]
    }

    void "test create data file submission objects"() {
        given:
        EgaSubmission submission = createEgaSubmission()
        List<Boolean> selectBox = [true, null]
        List<String> filenames = [DomainFactory.createDataFile().fileName, DomainFactory.createDataFile().fileName]
        List<String> egaSampleAliases = [
                createSampleSubmissionObject().egaAliasName,
                createSampleSubmissionObject().egaAliasName,
        ]

        when:
        egaSubmissionService.createDataFileSubmissionObjects(submission, selectBox, filenames, egaSampleAliases)

        then:
        submission.dataFilesToSubmit.size() == DataFileSubmissionObject.findAll().size()
    }

    void "test update data file submission objects"() {
        given:
        EgaSubmission submission = createEgaSubmission()
        List<String> egaFileAliases = ["someMagicAlias"]
        DataFile dataFile = DomainFactory.createDataFile()
        List<String> fileNames = [dataFile.fileName]
        DataFileSubmissionObject dataFileSubmissionObject = createDataFileSubmissionObject(
                dataFile: dataFile,
        )
        submission.addToDataFilesToSubmit(dataFileSubmissionObject)

        when:
        egaSubmissionService.updateDataFileSubmissionObjects(fileNames, egaFileAliases, submission)

        then:
        dataFileSubmissionObject.egaAliasName == egaFileAliases.first()
    }

    void "test update bam file submission objects"() {
        given:
        EgaSubmission submission = createEgaSubmission()
        List<String> egaFileAliases = ["someMagicAlias"]
        RoddyBamFile roddyBamFile = createBamFile()
        List<String> fileIds = [roddyBamFile.id.toString()]
        BamFileSubmissionObject bamFileSubmissionObject = createBamFileSubmissionObject(
                bamFile: roddyBamFile,
        )
        submission.addToBamFilesToSubmit(bamFileSubmissionObject)

        when:
        egaSubmissionService.updateBamFileSubmissionObjects(fileIds, egaFileAliases, submission)

        then:
        bamFileSubmissionObject.egaAliasName == egaFileAliases.first()
    }

    @Unroll
    void "test generate default ega aliases for data files"() {
        given:
        Run run = DomainFactory.createRun(
                name: runName
        )
        DataFile dataFile = DomainFactory.createDataFile(
                run: run
        )

        String alias = "someAlias"
        List dataFilesAndAliases = [new DataFileAndSampleAlias(dataFile, alias)]
        List aliasNameHelper = [
                dataFile.seqType.displayName,
                dataFile.seqType.libraryLayout,
                alias,
                runNameWithoutDate,
                dataFile.seqTrack.laneId,
                "R${dataFile.mateNumber}",
        ]

        when:
        Map defaultEgaAliasesForDataFiles = egaSubmissionService.generateDefaultEgaAliasesForDataFiles(dataFilesAndAliases)

        then:
        defaultEgaAliasesForDataFiles.get(dataFile.fileName + dataFile.run) == "${aliasNameHelper.join("_")}.fastq.gz"

        where:
        runName                            | runNameWithoutDate
        "120111_SN509_0137_BD0CWYACXX"     | "SN509_0137_BD0CWYACXX"
        "solid0719_20100818_PE_MB3_a"      | "solid0719_PE_MB3_a"
        "NB501263_25_HVTWTBGXX"            | "NB501263_25_HVTWTBGXX"
        "SN678_373_C7RM5ACXX"              | "SN678_373_C7RM5ACXX"
        "Wgbs-oakes-run9"                  | "Wgbs-oakes-run9"
        "run170629_D00392_0173_BCB26BANXX" | "runD00392_0173_BCB26BANXX"
        "Rna-oakes-run4"                   | "Rna-oakes-run4"
        "C6B02ACXX"                        | "C6B02ACXX"
        "ega_rnaseq_62JP4AAXX"             | "ega_rnaseq_62JP4AAXX"
        "NB501263_92_HCG7FBGX3"            | "NB501263_92_HCG7FBGX3"
        "NB501764_HVJ5JBGX5"               | "NB501764_HVJ5JBGX5"
        "2011-09-10-C05PPACXX"             | "C05PPACXX"
        "2011-010-10-C05PPACXX"            | "2011-010-10-C05PPACXX"
    }

    void "test generate default ega aliases for bam files"() {
        given:
        RoddyBamFile bamFile = createBamFile()

        String alias = "someAlias"
        List bamFilesAndAliases = [new BamFileAndSampleAlias(bamFile, alias)]
        List aliasNameHelper = [
                bamFile.seqType.displayName,
                bamFile.seqType.libraryLayout,
                alias,
                bamFile.md5sum,
        ]

        when:
        Map defaultEgaAliasesForDataFiles = egaSubmissionService.generateDefaultEgaAliasesForBamFiles(bamFilesAndAliases)

        then:
        defaultEgaAliasesForDataFiles.get(bamFile.bamFileName + alias) == "${aliasNameHelper.join("_")}.bam"
    }

    void "getDataFilesAndAlias, when already DataFiles are connected with the submission, then return these"() {
        given:
        SeqTrack seqTrack1 = createSeqTrackWithOneDataFile()
        SeqTrack seqTrack2 = createSeqTrackWithOneDataFile([
                sample : seqTrack1.sample,
                seqType: seqTrack1.seqType,
        ])
        DataFile datafile = CollectionUtils.exactlyOneElement(seqTrack2.dataFiles)
        EgaSubmission egaSubmission = createEgaSubmission([
                samplesToSubmit: [
                        createSampleSubmissionObject([
                                sample      : seqTrack1.sample,
                                seqType     : seqTrack1.seqType,
                                useFastqFile: true,
                        ]),
                ] as Set,
                dataFilesToSubmit: [
                        createDataFileSubmissionObject([
                                dataFile: datafile,
                        ]),
                ] as Set,
        ])
        egaSubmissionService.seqTrackService = new SeqTrackService([
                fileTypeService: new FileTypeService(),
        ])

        when:
        List<DataFileAndSampleAlias> list = egaSubmissionService.getDataFilesAndAlias(egaSubmission)

        then:
        list*.dataFile == [datafile]
    }

    void "getDataFilesAndAlias, when sample has two lane, then return both"() {
        given:
        SeqTrack seqTrack1 = createSeqTrackWithOneDataFile()
        SeqTrack seqTrack2 = createSeqTrackWithOneDataFile([
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

        List<DataFile> expectedDataFiles = [
                seqTrack1,
                seqTrack2,
        ]*.dataFiles.flatten()

        when:
        List<DataFileAndSampleAlias> list = egaSubmissionService.getDataFilesAndAlias(egaSubmission)

        then:
        TestCase.assertContainSame(list*.dataFile, expectedDataFiles)
    }
}
