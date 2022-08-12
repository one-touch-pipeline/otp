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
package de.dkfz.tbi.otp.job.jobs.rnaAlignment

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.SessionUtils

import java.nio.file.Path

@Rollback
@Integration
class ExecuteRnaAlignmentJobIntegrationSpec extends Specification implements RoddyRnaFactory {

    LsdfFilesService lsdfFilesService
    ReferenceGenomeService referenceGenomeService

    final static String ADAPTER_SEQUENCE1 = "ATGCCCTTGAATC"

    @TempDir
    Path tempDir
    TestConfigService configService

    void setup() {
        SessionUtils.metaClass.static.withNewSession = { Closure c -> c() }
    }

    void setupData() {
        configService.addOtpProperties(tempDir)
    }

    void cleanup() {
        TestCase.removeMetaClass(SessionUtils)
        configService.clean()
    }

    void "test prepareAndReturnWorkflowSpecificCValues no adapter sequence available"() {
        given:
        setupData()
        ExecuteRnaAlignmentJob executeRnaAlignmentJob = new ExecuteRnaAlignmentJob()
        executeRnaAlignmentJob.fileSystemService = new TestFileSystemService()
        executeRnaAlignmentJob.lsdfFilesService = lsdfFilesService
        executeRnaAlignmentJob.referenceGenomeService = referenceGenomeService

        RnaRoddyBamFile roddyBamFile = setUpForPrepareAndReturnWorkflowSpecificCValues()

        when:
        executeRnaAlignmentJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile)

        then:
        true
        AssertionError e = thrown()
        e.message.contains("There is not exactly one reverse complement adapter sequence available for BAM file")
    }

    void "test prepareAndReturnWorkflowSpecificCValues exactly one adapter sequence available"() {
        given:
        setupData()
        ExecuteRnaAlignmentJob executeRnaAlignmentJob = new ExecuteRnaAlignmentJob()
        executeRnaAlignmentJob.lsdfFilesService = lsdfFilesService
        executeRnaAlignmentJob.referenceGenomeService = referenceGenomeService

        RnaRoddyBamFile roddyBamFile = setUpForPrepareAndReturnWorkflowSpecificCValues()
        roddyBamFile.containedSeqTracks.each {
            LibraryPreparationKit libraryPreparationKit = it.libraryPreparationKit
            libraryPreparationKit.reverseComplementAdapterSequence = ADAPTER_SEQUENCE1
            assert libraryPreparationKit.save(flush: true)
        }
        executeRnaAlignmentJob.fileSystemService = new TestFileSystemService()

        when:
        List<String> cValues = executeRnaAlignmentJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile)

        then:
        noExceptionThrown()
        cValues.contains("INDEX_PREFIX:${executeRnaAlignmentJob.referenceGenomeService.fastaFilePath(roddyBamFile.referenceGenome) as File}")
        cValues.contains("possibleControlSampleNamePrefixes:$roddyBamFile.sampleType.dirName")
        cValues.contains("possibleTumorSampleNamePrefixes:")
        cValues.contains("fastq_list:${executeRnaAlignmentJob.getFilesToMerge(roddyBamFile).join(';')}")
        cValues.contains("ADAPTER_SEQ:${roddyBamFile.seqTracks.first().libraryPreparationKit.reverseComplementAdapterSequence}")
        cValues.contains("outputBaseDirectory:${roddyBamFile.workDirectory}")
        cValues.contains("ALIGNMENT_DIR:${roddyBamFile.workDirectory}")

        cleanup:
        executeRnaAlignmentJob.fileSystemService = null
    }

    private RnaRoddyBamFile setUpForPrepareAndReturnWorkflowSpecificCValues() {
        ExecuteRnaAlignmentJob executeRnaAlignmentJob = new ExecuteRnaAlignmentJob()
        executeRnaAlignmentJob.lsdfFilesService = lsdfFilesService
        executeRnaAlignmentJob.referenceGenomeService = new ReferenceGenomeService()
        executeRnaAlignmentJob.referenceGenomeService.configService = configService
        executeRnaAlignmentJob.referenceGenomeService.processingOptionService = new ProcessingOptionService()

        DomainFactory.createProcessingOptionBasePathReferenceGenome(tempDir.toString())
        RnaRoddyBamFile roddyBamFile = createBamFile()
        roddyBamFile.containedSeqTracks.each { SeqTrack s ->
            s.dataFiles.each { DataFile dataFile ->
                File file = new File(executeRnaAlignmentJob.lsdfFilesService.getFileViewByPidPath(dataFile))
                CreateFileHelper.createFile(file)
                dataFile.fileSize = file.length()
                assert dataFile.save(flush: true)
            }
        }
        CreateFileHelper.createFile(executeRnaAlignmentJob.referenceGenomeService.fastaFilePath(roddyBamFile.referenceGenome, false))

        return roddyBamFile
    }
}
