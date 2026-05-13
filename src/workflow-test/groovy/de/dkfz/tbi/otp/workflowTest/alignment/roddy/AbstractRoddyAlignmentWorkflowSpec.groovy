/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflowTest.alignment.roddy

import grails.converters.JSON
import org.grails.web.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPanCancerFactory
import de.dkfz.tbi.otp.infrastructure.alignment.PanCancerLinkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.PanCancerWorkFileService
import de.dkfz.tbi.otp.job.processing.RoddyConfigService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import de.dkfz.tbi.otp.workflow.alignment.roddy.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.OtpWorkflow
import de.dkfz.tbi.otp.workflowTest.alignment.AbstractAlignmentWorkflowSpec
import de.dkfz.tbi.otp.workflowTest.referenceGenome.UsingReferenceGenome
import de.dkfz.tbi.otp.workflowTest.roddy.RoddyReferences

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

/**
 * Base class for roddy alignment workflows
 */
abstract class AbstractRoddyAlignmentWorkflowSpec extends AbstractAlignmentWorkflowSpec implements UsingReferenceGenome, RoddyReferences, RoddyPanCancerFactory {

    // @Slf4j does not work with Spock containing tests and produces problems in closures
    @SuppressWarnings('PropertyName')
    final static Logger log = LoggerFactory.getLogger(AbstractRoddyAlignmentWorkflowSpec)

    AbstractBamFileService abstractBamFileService

    RoddyConfigService roddyConfigService

    PanCancerWorkFileService panCancerWorkFileService

    PanCancerLinkFileService panCancerLinkFileService

    Duration runningTimeout = Duration.ofHours(24)

    Class<? extends OtpWorkflow> workflowComponentClass = PanCancerWorkflow

    @Override
    void setup() {
        log.debug("Start setup ${this.class.simpleName}")
        SessionUtils.withTransaction {
            setUpFilesVariables()

            setUpDomainVariables()

            linkReferenceGenomeDirectoryToReference(referenceGenome)
            linkAdapterDirectoryToReference(libraryPreparationKit)
        }
        log.debug("Finish setup ${this.class.simpleName}")
    }

    protected void setUpFilesVariables() {
        testFastqFiles = [
                readGroup1: [
                        referenceDataDirectory.resolve('fastqFiles/wgs/normal/paired/run1/sequence/gerald_D1VCPACXX_6_R1.fastq.bz2'),
                        referenceDataDirectory.resolve('fastqFiles/wgs/normal/paired/run1/sequence/gerald_D1VCPACXX_6_R2.fastq.bz2'),
                ].asImmutable(),
                readGroup2: [
                        referenceDataDirectory.resolve('fastqFiles/wgs/normal/paired/run2/sequence/gerald_D1VCPACXX_7_R1.fastq.bz2'),
                        referenceDataDirectory.resolve('fastqFiles/wgs/normal/paired/run2/sequence/gerald_D1VCPACXX_7_R2.fastq.bz2'),
                ].asImmutable(),
        ].asImmutable()
    }

    protected void createStatSizeFileFragment() {
        workflowAlignment.refresh()
        referenceGenome.refresh()
        createFragmentAndSelector("statSizeFileFragment", """
                    {
                        "RODDY": {
                            "cvalues": {
                                "CHROM_SIZES_FILE": {
                                    "value": "\${BASE_REFERENCE_GENOME}/${referenceGenome.path}/stats/${chromosomeStatFileName}",
                                    "type": "path"
                                }
                            }
                        }
                    }
                """, [
                workflows       : [workflowAlignment],
                referenceGenomes: [referenceGenome],
        ])
    }

    protected void setUpFingerPrintingFile() {
        referenceGenome.refresh()
        referenceGenome.fingerPrintingFileName = fingerPrintingFileName
        referenceGenome.save(flush: true)
        assert referenceGenome.fingerPrintingFileName
        log.info("setup fingerPrintingFileName ${referenceGenome.fingerPrintingFileName}")
    }

    protected void setupUseAdapterTrimming() {
        workflowAlignment.refresh()
        createFragmentAndSelector("adapterTrimming", """
                    {
                        "RODDY": {
                            "cvalues": {
                                "useAdaptorTrimming": {
                                    "value": "true"
                                }
                            }
                        }
                    }
                """, [
                workflows: [workflowAlignment],
        ])
    }

    @Override
    protected void checkQC(AbstractBamFile bamFile) {
        RoddyBamFile roddyBamFile = bamFile as RoddyBamFile
        checkQC(roddyBamFile)
    }

    protected void checkQC(RoddyBamFile bamFile) {
        bamFile.seqTracks.each {
            List<RoddySingleLaneQa> qa = RoddySingleLaneQa.findAllBySeqTrack(it)
            assert qa
            qa.each {
                assert it.abstractBamFile == bamFile
            }
        }

        panCancerLinkFileService.getSingleLaneQAJsonFiles(bamFile).each { SeqTrack seqTrack, Path qaFile ->
            JSONObject json = (JSONObject) JSON.parse(Files.readString(qaFile))
            Iterator chromosomes = json.keys()
            chromosomes.each { String chromosome ->
                CollectionUtils.exactlyOneElement(RoddySingleLaneQa.findAllByChromosomeAndSeqTrack(chromosome, seqTrack))
            }
        }
        RoddyMergedBamQa mergedQa = CollectionUtils.exactlyOneElement(
                RoddyMergedBamQa.findAllByAbstractBamFileAndChromosome(bamFile, RoddyQualityAssessment.ALL))
        JSONObject json = (JSONObject) JSON.parse(Files.readString(panCancerLinkFileService.getMergedQAJsonFile(bamFile)))
        json.keys().each { String chromosome ->
            assert RoddyMergedBamQa.findAllByChromosomeAndAbstractBamFile(chromosome, bamFile)
        }
        assert bamFile.coverage == mergedQa.genomeWithoutNCoverageQcBases
        assert bamFile.coverageWithN == abstractBamFileService.calculateCoverageWithN(bamFile)

        assert bamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
        assert bamFile.qcTrafficLightStatus == AbstractBamFile.QcTrafficLightStatus.UNCHECKED

        if (bamFile.seqType.wgbs && bamFile.hasMultipleLibraries()) {
            List<RoddyLibraryQa> libraryQas = RoddyLibraryQa.findAllByAbstractBamFile(bamFile)
            assert libraryQas
            assert libraryQas*.libraryDirectoryName as Set == bamFile.seqTracks*.libraryDirectoryName as Set
        }
    }

    protected void checkLatestBamFileState(RoddyBamFile latestBamFile, Map latestBamFileProperties = [:]) {
        SeqTrack firstSeqTrack = CollectionUtils.exactlyOneElement(SeqTrack.findAllByLaneId("readGroup1"))
        SeqTrack secondSeqTrack = CollectionUtils.exactlyOneElement(SeqTrack.findAllByLaneId("readGroup2"))
        checkBamFileState(latestBamFile, [
                identifier         : 1,
                mostResentBamFile  : true,
                seqTracks          : [secondSeqTrack],
                containedSeqTracks : [firstSeqTrack, secondSeqTrack],
                fileOperationStatus: FileOperationStatus.PROCESSED,
                withdrawn          : false,
        ] + latestBamFileProperties)
    }

    @Override
    protected void assertBaseFileSystemState(AbstractBamFile bamFile) {
        RoddyBamFile roddyBamFile = bamFile as RoddyBamFile
        assertRoddyBaseFileSystemState(roddyBamFile)
    }

    protected void assertRoddyBaseFileSystemState(RoddyBamFile bamFile) {
        assertWorkDirectoryFileSystemState(bamFile)
        assertRoddyExecutionDirectories(bamFile)
        assertBamFileFileOnFileSystem(bamFile)
        assertWorkflowFileSystemState(bamFile)

        verifyInputIsNotDeleted()
    }

    protected void assertWorkDirectoryFileSystemState(RoddyBamFile bamFile) {
        //  content of the work dir: executionStoreDirectory
        fileAssertHelper.assertDirectoryContentReadable(panCancerWorkFileService.getExecutionDirectories(bamFile))

        // check that given files exist in the execution store:
        panCancerWorkFileService.getExecutionDirectories(bamFile).each { executionStore ->
            filesInRoddyExecutionDir.each { String fileName ->
                fileAssertHelper.assertFileIsReadableAndNotEmpty(executionStore.resolve(fileName))
            }
        }

        // check default json, additional needs to be checked in the subclass
        Path qaJson = getWorkMergedQAJsonFile(bamFile)
        fileAssertHelper.assertFileIsReadableAndNotEmpty(qaJson)
        JSON.parse(Files.readString(qaJson)) // throws ConverterException when the JSON content is not valid

        assertWorkflowWorkDirectoryFileSystemState(bamFile)
    }

    private void assertRoddyExecutionDirectories(RoddyBamFile bamFile) {
        List<Path> expectedRoddyExecutionDirs = panCancerLinkFileService.getExecutionDirectories(bamFile)
        fileAssertHelper.assertDirectoryContentReadable(expectedRoddyExecutionDirs)
    }

    private void assertBamFileFileOnFileSystem(RoddyBamFile bamFile) {
        // check md5sum content
        assert bamFile.md5sum == Files.readString(panCancerLinkFileService.getMd5sumFile(bamFile)).replaceAll("\n", "")

        // content of the bam file
        LogThreadLocal.withThreadLog(System.out) {
            LocalShellHelper.executeAndWait(" zcat  ${panCancerLinkFileService.getBamFile(bamFile)} 1> /dev/null").assertExitCodeZeroAndStderrEmpty()
        }
        assert Files.size(panCancerLinkFileService.getBamFile(bamFile)) == bamFile.fileSize

        // samtools may under some circumstances produce small bam files of size larger than zero that however do not contain any reads.
        assert Files.size(panCancerLinkFileService.getBamFile(bamFile)) > 1024L
    }

    @Override
    protected void checkBamFileConfig(AbstractBamFile bamFile) {
        RoddyBamFile roddyBamFile = bamFile as RoddyBamFile
        assert roddyBamFile.config == null
    }

    abstract protected Path getWorkMergedQAJsonFile(RoddyBamFile bamFile)

    abstract protected void assertWorkflowFileSystemState(RoddyBamFile bamFile)

    abstract protected void assertWorkflowWorkDirectoryFileSystemState(RoddyBamFile bamFile)
}
