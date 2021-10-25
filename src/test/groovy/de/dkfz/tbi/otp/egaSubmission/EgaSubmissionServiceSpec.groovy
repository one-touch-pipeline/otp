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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.submissions.ega.EgaSubmissionFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

class EgaSubmissionServiceSpec extends Specification implements EgaSubmissionFactory, IsRoddy, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
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
                FastqImportInstance,
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

    void "updatePubMedId, when submission is not null it should succeed"() {
        given:
        final EgaSubmission submission = createEgaSubmission()
        final String pubMedId = "123test"

        when:
        egaSubmissionService.updatePubMedId(submission, pubMedId)

        then:
        submission.pubMedId == pubMedId
    }

    void "createAndSaveSampleSubmissionObjects, when submission is null, then throw an assertion"() {
        given:
        Sample sample = createSample()
        SeqType seqType = createSeqType()
        String id = "${sample.id}-${seqType.id}"

        when:
        egaSubmissionService.createAndSaveSampleSubmissionObjects(null, [id])

        then:
        AssertionError e = thrown()
        e.message.contains('submission')
    }

    @Unroll
    void "createAndSaveSampleSubmissionObjects, when sampleIdSeqTypeIdList is #name, then throw an assertion"() {
        given:
        EgaSubmission submission = createEgaSubmission()

        when:
        egaSubmissionService.createAndSaveSampleSubmissionObjects(submission, sampleIdSeqTypeIdList)

        then:
        AssertionError e = thrown()
        e.message.contains('sampleIdSeqTypeIdList')

        where:
        name    | sampleIdSeqTypeIdList
        'null'  | null
        'empty' | []
    }

    @Unroll
    void "createAndSaveSampleSubmissionObjects, when a value in sampleIdSeqTypeIdList contains for #name id not a number, then throw a NumberFormatException"() {
        given:
        EgaSubmission submission = createEgaSubmission()

        when:
        egaSubmissionService.createAndSaveSampleSubmissionObjects(submission, [idString])

        then:
        NumberFormatException e = thrown()
        e.message.contains('For input string')

        where:
        name      | idString
        'seqtype' | '1-a'
        'sample'  | 'a-1'
    }

    @Unroll
    void "createAndSaveSampleSubmissionObjects, when a value in sampleIdSeqTypeIdList contains #name numbers, then throw an assertion"() {
        given:
        EgaSubmission submission = createEgaSubmission()

        when:
        egaSubmissionService.createAndSaveSampleSubmissionObjects(submission, [idString])

        then:
        AssertionError e = thrown()
        e.message.contains('sampleAndSeqType.size')

        where:
        name       | idString
        'only one' | '1'
        'three'    | '1-2-3'
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
    void "test generate default ega aliases for data files - should remove privacy-sensitive sequencing dates from filenames"() {
        given:
        Run run = DomainFactory.createRun(
                name: runName
        )
        DataFile dataFile = DomainFactory.createDataFile(
                run: run
        )

        String alias = "someAlias"
        List dataFilesAndAliases = [new DataFileAndSampleAlias(dataFile, new SampleSubmissionObject(egaAliasName: alias))]

        when:
        Map defaultEgaAliasesForDataFiles = egaSubmissionService.generateDefaultEgaAliasesForDataFiles(dataFilesAndAliases)

        then:
        List aliasNameHelper = [
                dataFile.seqType.displayName,
                dataFile.seqType.libraryLayout,
                alias,
                runNameWithoutDate,
                dataFile.seqTrack.laneId,
                "R${dataFile.mateNumber}",
        ]
        defaultEgaAliasesForDataFiles.get(dataFile.fileName + dataFile.run) == "${aliasNameHelper.join("_")}.fastq.gz"

        where:
        runName                            || runNameWithoutDate
        "120111_SN509_0137_BD0CWYACXX"     || "SN509_0137_BD0CWYACXX"
        "solid0719_20100818_PE_MB3_a"      || "solid0719_PE_MB3_a"
        "NB501263_25_HVTWTBGXX"            || "NB501263_25_HVTWTBGXX"
        "SN678_373_C7RM5ACXX"              || "SN678_373_C7RM5ACXX"
        "Wgbs-oakes-run9"                  || "Wgbs-oakes-run9"
        "run170629_D00392_0173_BCB26BANXX" || "runD00392_0173_BCB26BANXX"
        "Rna-oakes-run4"                   || "Rna-oakes-run4"
        "C6B02ACXX"                        || "C6B02ACXX"
        "ega_rnaseq_62JP4AAXX"             || "ega_rnaseq_62JP4AAXX"
        "NB501263_92_HCG7FBGX3"            || "NB501263_92_HCG7FBGX3"
        "NB501764_HVJ5JBGX5"               || "NB501764_HVJ5JBGX5"
        "2011-09-10-C05PPACXX"             || "C05PPACXX"
        "2011-010-10-C05PPACXX"            || "2011-010-10-C05PPACXX"
    }

    void "test generate default ega aliases for bam files"() {
        given:
        String alias = "someAlias"
        RoddyBamFile bamFile = createBamFile()
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject([
                egaAliasName: alias,
        ])

        List bamFilesAndAliases = [new BamFileAndSampleAlias(bamFile, sampleSubmissionObject)]
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
}
