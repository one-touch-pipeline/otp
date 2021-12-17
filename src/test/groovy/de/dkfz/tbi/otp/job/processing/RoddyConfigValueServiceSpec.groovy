/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.job.processing

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService

import java.nio.file.Paths

class RoddyConfigValueServiceSpec extends Specification implements ServiceUnitTest<RoddyConfigValueService>, DataTest, WorkflowSystemDomainFactory, IsRoddy {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqImportInstance,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingWorkPackage,
                Pipeline,
                ProcessingOption,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Sample,
                SampleType,
        ]
    }

    TestConfigService configService

    void setup() {
        configService = new TestConfigService()
    }

    void cleanup() {
        configService.clean()
    }

    void "test getDefaultValues"() {
        given:
        service.processingOptionService = new ProcessingOptionService()
        findOrCreateProcessingOption(name: ProcessingOption.OptionName.RODDY_SHARED_FILES_BASE_DIRECTORY, value: "/asdf")

        expect:
        service.defaultValues == ["sharedFilesBaseDirectory": "/asdf"]
    }

    void "test getConfigurationValues, with whole genome seq. type, with fingerprinting"() {
        given:
        RoddyBamFile roddyBamFile = createBamFile()
        roddyBamFile.referenceGenome.fingerPrintingFileName = "fingerprintingFile"
        roddyBamFile.referenceGenome.save(flush: true)

        service.referenceGenomeService = Mock(ReferenceGenomeService) {
            fastaFilePath(roddyBamFile.referenceGenome) >> { new File("/fasta-path") }
            chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage) >> { new File("/chrom-size-path") }
            fingerPrintingFile(roddyBamFile.referenceGenome) >> { new File("/fingerprint-path") }
        }

        Map<String, String> expectedCommand = [
                "INDEX_PREFIX"                     : "/fasta-path",
                "GENOME_FA"                        : "/fasta-path",
                "CHROM_SIZES_FILE"                 : "/chrom-size-path",
                "possibleControlSampleNamePrefixes": "${roddyBamFile.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes"  : "",
                "runFingerprinting"                : "true",
                "fingerprintingSitesFile"          : "/fingerprint-path",
        ]

        when:
        Map<String, String> actualCommand = service.getAlignmentValues(roddyBamFile, "{}")

        then:
        new HashMap(expectedCommand) == new HashMap(actualCommand)
    }

    void "test getConfigurationValues, with whole genome seq. type"() {
        given:
        RoddyBamFile roddyBamFile = createBamFile()

        service.referenceGenomeService = Mock(ReferenceGenomeService) {
            fastaFilePath(roddyBamFile.referenceGenome) >> { new File("/fasta-path") }
            chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage) >> { new File("/chrom-size-path") }
        }

        Map<String, String> expectedCommand = [
                "INDEX_PREFIX"                     : "/fasta-path",
                "GENOME_FA"                        : "/fasta-path",
                "CHROM_SIZES_FILE"                 : "/chrom-size-path",
                "possibleControlSampleNamePrefixes": "${roddyBamFile.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes"  : "",
                "runFingerprinting"                : "false",
        ]

        when:
        Map<String, String> actualCommand = service.getAlignmentValues(roddyBamFile, "{}")

        then:
        new HashMap(expectedCommand) == new HashMap(actualCommand)
    }

    void "test getConfigurationValues, with RNA seq. type"() {
        given:
        RoddyBamFile roddyBamFile = createBamFile()
        roddyBamFile.mergingWorkPackage.seqType = DomainFactory.createRnaPairedSeqType()

        service.referenceGenomeService = Mock(ReferenceGenomeService) {
            fastaFilePath(roddyBamFile.referenceGenome) >> { new File("/fasta-path") }
        }

        Map<String, String> expectedCommand = [
                "INDEX_PREFIX"                     : "/fasta-path",
                "GENOME_FA"                        : "/fasta-path",
                "possibleControlSampleNamePrefixes": "${roddyBamFile.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes"  : "",
                "runFingerprinting"                : "false",
        ]

        when:
        Map<String, String> actualCommand = service.getAlignmentValues(roddyBamFile, "{}")

        then:
        new HashMap(expectedCommand) == new HashMap(actualCommand)
    }

    void "test getAdapterTrimmingFile, adapter trimming disabled"() {
        given:
        RoddyBamFile roddyBamFile = createBamFile()

        expect:
        [:] == service.getAdapterTrimmingFile(roddyBamFile, "{}")
    }

    void "test getAdapterTrimmingFile, adapter trimming enabled"() {
        given:
        RoddyBamFile roddyBamFile = createBamFile()

        String path = "/adapter-file"
        roddyBamFile.containedSeqTracks*.libraryPreparationKit*.adapterFile = path
        roddyBamFile.containedSeqTracks*.libraryPreparationKit*.save(flush: true)

        String config = '{"RODDY": {"cvalues": {"useAdaptorTrimming": {"value": "true"}}}}'

        expect:
        ["CLIP_INDEX": path] == service.getAdapterTrimmingFile(roddyBamFile, config)
    }

    void "test getFilesToMerge"() {
        given:
        service.lsdfFilesService = new LsdfFilesService()
        service.lsdfFilesService.fileService = new FileService()
        service.lsdfFilesService.individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> { Paths.get("/viewbypidpath") }
        }
        RoddyBamFile roddyBamFile = createBamFile()

        expect:
        ["fastq_list": fastqFilesAsString(roddyBamFile)] == service.getFilesToMerge(roddyBamFile)
    }

    private String fastqFilesAsString(RoddyBamFile roddyBamFileToUse) {
        return roddyBamFileToUse.seqTracks.collectMany { SeqTrack seqTrack ->
            DataFile.findAllBySeqTrack(seqTrack).collect { DataFile dataFile ->
                service.lsdfFilesService.getFileViewByPidPathAsPath(dataFile).toString()
            }
        }.join(';')
    }
}
