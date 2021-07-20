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
package de.dkfz.tbi.otp.fastqc

import spock.lang.Shared
import spock.lang.Unroll

import de.dkfz.tbi.otp.WorkflowTestCase
import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.SessionUtils

import java.time.Duration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class FastqcWorkflowTests extends WorkflowTestCase {

    LsdfFilesService lsdfFilesService
    FastqcDataFilesService fastqcDataFilesService

    @Shared
    File sourceFastq
    @Shared
    File expectedFastqc
    @Shared
    DataFile dataFile
    @Shared
    SeqCenter seqCenter
    @Shared
    SeqTrack seqTrack

    void setupWorkflow(String fileExtension) {
        sourceFastq = new File(inputRootDirectory, "fastqFiles/fastqc/input_fastqc.fastq.${fileExtension}")
        expectedFastqc = new File(inputRootDirectory, "fastqFiles/fastqc/asdf_fastqc.zip")

        Project project = DomainFactory.createProject(
                realm: realm
        )

        Individual individual = DomainFactory.createIndividual(
                project: project,
                type: Individual.Type.REAL,
        )

        Sample sample = DomainFactory.createSample(individual: individual)

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

    void "test FastQcWorkflow, when FastQC result file is available"() {
        given:
        SessionUtils.withNewSession {
            setupWorkflow('gz')
            String initialPath = new File(lsdfFilesService.getFileInitialPath(dataFile)).parent
            String fastqcFileName = fastqcDataFilesService.fastqcFileName(dataFile)
            linkFileUtils.createAndValidateLinks([(expectedFastqc): new File("${initialPath}/${fastqcFileName}")], realm)
        }

        when:
        execute()

        then:
        checkExistenceOfResultsFiles()
        validateFastqcProcessedFile()
        validateFastQcFileContent()
    }

    @Unroll
    void "test FastQcWorkflow, when FastQC result file is not available and extension is #extension"() {
        given:
        SessionUtils.withNewSession {
            setupWorkflow(extension)
        }

        when:
        execute()

        then:
        checkExistenceOfResultsFiles()
        validateFastqcProcessedFile()
        validateFastQcFileContent()

        where:
        extension | _
        'gz'      | _
        'tar.gz'  | _
        'bz2'     | _
        'tar.bz2' | _
    }

    private void checkExistenceOfResultsFiles() {
        SessionUtils.withNewSession {
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
    }

    private void validateFastqcProcessedFile() {
        SessionUtils.withNewSession {
            FastqcProcessedFile fastqcProcessedFile = CollectionUtils.exactlyOneElement(FastqcProcessedFile.all)

            assert FastqcProcessedFile.all.size() == 1
            assert fastqcProcessedFile.fileExists
            assert fastqcProcessedFile.contentUploaded
            assert fastqcProcessedFile.dataFile == dataFile
        }
    }

    private void validateFastQcFileContent() {
        SessionUtils.withNewSession {
            dataFile.refresh()
            assert null != dataFile.sequenceLength
            assert null != dataFile.nReads
            seqTrack.refresh()
            assert seqTrack.nBasePairs
        }
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
