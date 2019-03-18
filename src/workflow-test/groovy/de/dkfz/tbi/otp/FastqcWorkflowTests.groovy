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

package de.dkfz.tbi.otp

import org.junit.Ignore
import org.junit.Test

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.time.Duration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@Ignore
class FastqcWorkflowTests extends WorkflowTestCase {

    ProcessingOptionService processingOptionService
    LsdfFilesService lsdfFilesService
    FastqcDataFilesService fastqcDataFilesService


    File sourceFastq
    File expectedFastqc
    DataFile dataFile
    SeqCenter seqCenter
    SeqTrack seqTrack


    void setUpWorkFlow(String fileExtension) {
        sourceFastq = new File(inputRootDirectory, "fastqFiles/fastqc/input_fastqc.fastq.${fileExtension}")
        expectedFastqc = new File(inputRootDirectory, "fastqFiles/fastqc/asdf_fastqc.zip")

        Project project = Project.build(
                realm: realm
        )

        Individual individual = Individual.build(
                project: project,
                type: Individual.Type.REAL,
        )

        Sample sample = Sample.build(individual: individual)

        Run run = DomainFactory.createRun()

        seqCenter = run.seqCenter

        seqTrack = DomainFactory.createSeqTrack(
                sample: sample,
                fastqcState: SeqTrack.DataProcessingState.NOT_STARTED,
                seqType: SeqTypeService.wholeGenomePairedSeqType,
                run: run,
        )

        dataFile = DomainFactory.createSequenceDataFile(
                fileExists: true,
                fileSize: 1,
                fileName: "asdf.fastq.${fileExtension}",
                vbpFileName: "asdf.fastq.${fileExtension}",
                seqTrack: seqTrack,
                run: run,
                initialDirectory: "${ftpDir}/${run.name}",
        )

        linkFileUtils.createAndValidateLinks([(sourceFastq): new File(lsdfFilesService.getFileFinalPath(dataFile))], realm)
    }


    @Test
    void testWorkflow_FastQcDataAvailable() {
        setUpWorkFlow('gz')
        String initialPath = new File(lsdfFilesService.getFileInitialPath(dataFile)).parent
        String fastqcFileName = fastqcDataFilesService.fastqcFileName(dataFile)

        linkFileUtils.createAndValidateLinks([(expectedFastqc): new File("${initialPath}/${fastqcFileName}")], realm)

        execute()

        checkExistenceOfResultsFiles()
        validateFastqcProcessedFile()
        validateFastQcFileContent()
    }

    @Test
    void testWorkflow_FastQcDataNotAvailable() {
        setUpWorkFlow('gz')
        execute()

        checkExistenceOfResultsFiles()
        validateFastqcProcessedFile()
        validateFastQcFileContent()
    }

    @Test
    void testWorkflow_FastQcDataNotAvailable_bzip2() {
        setUpWorkFlow('bz2')
        execute()

        checkExistenceOfResultsFiles()
        validateFastqcProcessedFile()
        validateFastQcFileContent()
    }

    @Test
    void testWorkflow_FastQcDataNotAvailable_tar_bzip2() {
        setUpWorkFlow('tar.bz2')
        execute()

        checkExistenceOfResultsFiles()
        validateFastqcProcessedFile()
        validateFastQcFileContent()
    }

    private validateFastQcFileContent() {
        dataFile.refresh()
        assert null != dataFile.sequenceLength
        assert null != dataFile.nReads
        seqTrack.refresh()
        assert seqTrack.nBasePairs
    }

    private void checkExistenceOfResultsFiles() {
        ZipFile expectedResult = new ZipFile(expectedFastqc)
        ZipFile actualResult = new ZipFile(fastqcDataFilesService.fastqcOutputFile(dataFile))

        List<String> actualFiles = []
        actualResult.entries().each { ZipEntry entry ->
            actualFiles.add(entry.name)
        }

        expectedResult.entries().each { ZipEntry entry ->
            assert actualFiles.contains(entry.name)
            actualFiles.remove(entry.name)
        }
        assert actualFiles.isEmpty()
    }

    private validateFastqcProcessedFile() {
        FastqcProcessedFile fastqcProcessedFile = CollectionUtils.exactlyOneElement(FastqcProcessedFile.all)

        assert FastqcProcessedFile.all.size() == 1
        assert fastqcProcessedFile.fileExists
        assert fastqcProcessedFile.contentUploaded
        assert fastqcProcessedFile.dataFile == dataFile
    }

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/FastqcWorkflow.groovy",
        ]
    }

    @Override
    Duration getTimeout() {
        Duration.ofMinutes(20)
    }
}
