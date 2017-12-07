package de.dkfz.tbi.otp.job.jobs.rnaAlignment

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.spock.*
import org.junit.*
import org.junit.rules.*

class ExecuteRnaAlignmentJobIntegrationSpec extends IntegrationSpec {

    ExecuteRnaAlignmentJob executeRnaAlignmentJob

    final static String ADAPTER_SEQUENCE1 = "ATGCCCTTGAATC"

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()
    TestConfigService configService

    void setup() {
        configService = new TestConfigService([
                        'otp.root.path': tmpDir.root.path,
                        'otp.processing.root.path': tmpDir.root.path
        ])
    }

    void cleanup() {
        configService.clean()
    }


    void "test prepareAndReturnWorkflowSpecificCValues no adapter sequence available"() {
        given:
        RnaRoddyBamFile roddyBamFile = setUpForPrepareAndReturnWorkflowSpecificCValues()

        when:
        executeRnaAlignmentJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile)

        then:
        AssertionError e = thrown()
        e.message.contains("There is not exactly one reverse complement adapter sequence available for BAM file")
    }

    void "test prepareAndReturnWorkflowSpecificCValues exactly one adapter sequence available"() {
        given:
        RnaRoddyBamFile roddyBamFile = setUpForPrepareAndReturnWorkflowSpecificCValues()
        roddyBamFile.containedSeqTracks.each {
            LibraryPreparationKit libraryPreparationKit = it.libraryPreparationKit
            libraryPreparationKit.reverseComplementAdapterSequence = ADAPTER_SEQUENCE1
            assert libraryPreparationKit.save(flush: true)
        }

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
    }


    private RnaRoddyBamFile setUpForPrepareAndReturnWorkflowSpecificCValues() {
        executeRnaAlignmentJob = new ExecuteRnaAlignmentJob()
        executeRnaAlignmentJob.lsdfFilesService = new LsdfFilesService()
        executeRnaAlignmentJob.lsdfFilesService.configService = configService
        executeRnaAlignmentJob.referenceGenomeService = new ReferenceGenomeService()
        executeRnaAlignmentJob.referenceGenomeService.configService = executeRnaAlignmentJob.lsdfFilesService.configService
        executeRnaAlignmentJob.referenceGenomeService.processingOptionService = new ProcessingOptionService()

        DomainFactory.createProcessingOptionBasePathReferenceGenome(tmpDir.root.absolutePath)
        RnaRoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([:], RnaRoddyBamFile)
        DomainFactory.createRealmDataManagement([name: roddyBamFile.project.realmName])
        DomainFactory.createRealmDataProcessing([name: roddyBamFile.project.realmName])
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
