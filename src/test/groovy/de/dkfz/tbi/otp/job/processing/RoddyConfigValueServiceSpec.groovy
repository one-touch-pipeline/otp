/*
 * Copyright 2011-2025 The OTP authors
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
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Paths

class RoddyConfigValueServiceSpec extends Specification implements ServiceUnitTest<RoddyConfigValueService>, DataTest, WorkflowSystemDomainFactory, IsRoddy {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
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
    RoddyConfigValueService service

    void setup() {
        configService = new TestConfigService()
        service = new RoddyConfigValueService()
    }

    void cleanup() {
        configService.clean()
    }

    void "test getDefaultValues"() {
        given:
        service.processingOptionService = new ProcessingOptionService()
        findOrCreateProcessingOption(name: ProcessingOption.OptionName.BASE_PATH_REFERENCE_GENOME, value: "${File.separator}qwertz")
        findOrCreateProcessingOption(name: ProcessingOption.OptionName.RODDY_SHARED_FILES_BASE_DIRECTORY, value: "${File.separator}asdf")

        expect:
        service.defaultValues ==
                [
                        "BASE_REFERENCE_GENOME"   : [value: "${File.separator}qwertz", type: "path"],
                        "sharedFilesBaseDirectory": [value: "${File.separator}asdf", type: "path"],
                ]
    }

    void "test getConfigurationValues, with whole genome seq. type, with fingerprinting"() {
        given:
        RoddyBamFile roddyBamFile = createBamFile()
        roddyBamFile.referenceGenome.fingerPrintingFileName = "fingerprintingFile"
        roddyBamFile.referenceGenome.save(flush: true)

        service.referenceGenomeService = Mock(ReferenceGenomeService) {
            _ * fastaFilePath(roddyBamFile.referenceGenome) >> { Paths.get("/fasta-path").toFile() }
            _ * fingerPrintingFile(roddyBamFile.referenceGenome) >> { Paths.get("/fingerprint-path").toFile() }
        }

        Map<String, String> expectedCommand = [
                INDEX_PREFIX                     : [value: Paths.get("/fasta-path").toAbsolutePath().toString(), type: "path"],
                GENOME_FA                        : [value: Paths.get("/fasta-path").toAbsolutePath().toString(), type: "path"],
                possibleControlSampleNamePrefixes: [value: "${roddyBamFile.sampleType.dirName}"],
                possibleTumorSampleNamePrefixes  : [value: ""],
                runFingerprinting                : [value: "true", type: "boolean"],
                fingerprintingSitesFile          : [value: Paths.get("/fingerprint-path").toAbsolutePath().toString(), type: "path"],
        ]

        when:
        Map<String, String> actualCommand = service.getAlignmentValues(roddyBamFile, "{}")

        then:
        TestCase.assertContainSame(expectedCommand, actualCommand)
    }

    void "test getConfigurationValues, with whole genome seq. type"() {
        given:
        RoddyBamFile roddyBamFile = createBamFile()

        service.referenceGenomeService = Mock(ReferenceGenomeService) {
            fastaFilePath(roddyBamFile.referenceGenome) >> { Paths.get("/fasta-path").toFile() }
        }

        Map<String, String> expectedCommand = [
                INDEX_PREFIX                     : [value: Paths.get("/fasta-path").toAbsolutePath().toString(), type: "path"],
                GENOME_FA                        : [value: Paths.get("/fasta-path").toAbsolutePath().toString(), type: "path"],
                possibleControlSampleNamePrefixes: [value: "${roddyBamFile.sampleType.dirName}"],
                possibleTumorSampleNamePrefixes  : [value: ""],
                runFingerprinting                : [value: "false", type: "boolean"],
        ]

        when:
        Map<String, String> actualCommand = service.getAlignmentValues(roddyBamFile, "{}")

        then:
        TestCase.assertContainSame(expectedCommand, actualCommand)
    }

    void "test getConfigurationValues, with RNA seq. type"() {
        given:
        RoddyBamFile roddyBamFile = createBamFile()
        roddyBamFile.mergingWorkPackage.seqType = DomainFactory.createRnaPairedSeqType()

        service.referenceGenomeService = Mock(ReferenceGenomeService) {
            fastaFilePath(roddyBamFile.referenceGenome) >> { Paths.get("/fasta-path").toFile() }
        }

        Map<String, String> expectedCommand = [
                INDEX_PREFIX                     : [value: Paths.get("/fasta-path").toAbsolutePath().toString(), type: "path"],
                GENOME_FA                        : [value: Paths.get("/fasta-path").toAbsolutePath().toString(), type: "path"],
                possibleControlSampleNamePrefixes: [value: "${roddyBamFile.sampleType.dirName}"],
                possibleTumorSampleNamePrefixes  : [value: ""],
                runFingerprinting                : [value: "false", type: "boolean"],
        ]

        when:
        Map<String, String> actualCommand = service.getAlignmentValues(roddyBamFile, "{}")

        then:
        TestCase.assertContainSame(expectedCommand, actualCommand)
    }

    void "test getAdapterTrimmingFileForPanCancer, adapter trimming disabled"() {
        given:
        RoddyBamFile roddyBamFile = createBamFile()

        expect:
        service.getAdapterTrimmingFileForPanCancer(roddyBamFile, "{}") == [:]
    }

    void "test getAdapterTrimmingFileForPanCancer, adapter trimming enabled"() {
        given:
        RoddyBamFile roddyBamFile = createBamFile()

        String path = Paths.get("/adapter-file")
        roddyBamFile.containedSeqTracks*.libraryPreparationKit*.adapterFile = path
        roddyBamFile.containedSeqTracks*.libraryPreparationKit*.save(flush: true)

        String config = '{"RODDY": {"cvalues": {"useAdaptorTrimming": {"value": "true"}}}}'

        expect:
        service.getAdapterTrimmingFileForPanCancer(roddyBamFile, config) == [CLIP_INDEX: [value: path, type: "path"]]
    }

    void "test getRunArriba returns true, when both RUN_ARRIBA and useSingleEndProcessing are null"() {
        given:
        String config = '{"RODDY": {"cvalues": {}}}'
        WorkflowRun workflowRun = createWorkflowRun(combinedConfig: config)
        WorkflowStep workflowStep = createWorkflowStep(workflowRun: workflowRun)

        expect:
        service.getRunArriba(workflowStep)
    }

    @Unroll
    void "test getRunArriba returns #result, when RUN_ARRIBA is null and useSingleEndProcessing is #useSingleEndProcessing"() {
        given:
        String config = '{"RODDY": {"cvalues": {"useSingleEndProcessing": {"value": "' + useSingleEndProcessing + '"}}}}'
        WorkflowRun workflowRun = createWorkflowRun(combinedConfig: config)
        WorkflowStep workflowStep = createWorkflowStep(workflowRun: workflowRun)

        expect:
        service.getRunArriba(workflowStep) == result

        where:
        useSingleEndProcessing || result
        "true"                 || false
        "false"                || true
    }

    @Unroll
    void "test getRunArriba returns #result, when RUN_ARRIBA is #runArriba and useSingleEndProcessing is null"() {
        given:
        String config = '{"RODDY": {"cvalues": {"RUN_ARRIBA": {"value": "' + runArriba + '"}}}}'
        WorkflowRun workflowRun = createWorkflowRun(combinedConfig: config)
        WorkflowStep workflowStep = createWorkflowStep(workflowRun: workflowRun)

        expect:
        service.getRunArriba(workflowStep) == result

        where:
        runArriba || result
        "true"    || true
        "false"   || false
    }

    @Unroll
    void "test getRunArriba returns #result, when RUN_ARRIBA is #runArriba and useSingleEndProcessing is #useSingleEndProcessing"() {
        given:
        String config = '{"RODDY": {"cvalues": {"useSingleEndProcessing": {"value": "' + useSingleEndProcessing + '"}, "RUN_ARRIBA": {"value": "' + runArriba + '"}}}}'
        WorkflowRun workflowRun = createWorkflowRun(combinedConfig: config)
        WorkflowStep workflowStep = createWorkflowStep(workflowRun: workflowRun)

        expect:
        service.getRunArriba(workflowStep) == result

        where:
        runArriba | useSingleEndProcessing || result
        "true"    | "true"                 || false
        "true"    | "false"                || true
        "false"   | "true"                 || false
        "false"   | "false"                || false
    }

    void "test getFilesToMerge"() {
        given:
        service.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            2 * getFilePath(_) >> { RawSequenceFile rawSequenceFile -> Paths.get("/completePath", rawSequenceFile.fileName).toAbsolutePath() }
        }
        RoddyBamFile roddyBamFile = createBamFile()
        createBamFile()

        expect:
        service.getFilesToMerge(roddyBamFile) == [
                fastq_list: [value: "${Paths.get('/completePath', 'DataFileFileName_R1.gz').toAbsolutePath()};" +
                        "${Paths.get('/completePath', 'DataFileFileName_R2.gz').toAbsolutePath()}"]]
    }

    void "createValueMap, when value given, return map without type"() {
        given:
        String value = "value"

        expect:
        service.createValueMap(value) == [value: value]
    }

    void "createPathValueMap, when value given, return map with type 'path'"() {
        given:
        String value = "path"

        expect:
        service.createPathValueMap(value) == [value: value, type: 'path']
    }

    @Unroll
    void "createBooleanValueMap, when value is '#value', return map with type 'boolean'"() {
        expect:
        service.createBooleanValueMap(value) == [value: value, type: 'boolean']

        where:
        value << [
                true,
                false,
        ]*.toString()
    }

    @Unroll
    void "createBooleanValueMap, when value is '#value', throw assertion error"() {
        when:
        service.createBooleanValueMap(value)

        then:
        thrown(AssertionError)

        where:
        value << [
                null,
                '',
                '0',
                '1',
                't',
                'f',
        ]
    }

    void "createBashArrayValueMap, when value given, return map with type 'bashArray'"() {
        given:
        String value = "bashArray"

        expect:
        service.createBashArrayValueMap(value) == [value: value, type: 'bashArray']
    }
}
