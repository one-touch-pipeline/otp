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
package de.dkfz.tbi.otp.job.jobs

import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.jobs.runYapsa.ExecuteRunYapsaJob
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.Path
import java.nio.file.Paths

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

class ExecuteRunYapsaJobSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractMergedBamFile,
                AbstractMergingWorkPackage,
                AbstractSnvCallingInstance,
                DataFile,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingWorkPackage,
                Pipeline,
                ProcessingOption,
                ProcessingStep,
                Project,
                Realm,
                ReferenceGenome,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddySnvCallingInstance,
                RoddyWorkflowConfig,
                Run,
                FastqImportInstance,
                RunYapsaConfig,
                RunYapsaInstance,
                Sample,
                SamplePair,
                SampleType,
                SampleTypePerProject,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
        ]
    }

    static final int MIN_CONFIDENCE_SCORE = 8

    @Rule
    TemporaryFolder temporaryFolder

    RunYapsaInstance setupData() {
        DomainFactory.createProcessingOptionLazy(
                name: COMMAND_LOAD_MODULE_LOADER,
                value: "module load",
        )
        DomainFactory.createProcessingOptionLazy(
                name: COMMAND_ACTIVATION_R,
                value: "load r",
        )
        DomainFactory.createProcessingOptionLazy(
                name: COMMAND_ENABLE_MODULE,
                value: "load",
        )

        RunYapsaInstance instance = DomainFactory.createRunYapsaInstanceWithRoddyBamFiles()

        DomainFactory.createRoddySnvCallingInstance(instance.samplePair, [
                sampleType1BamFile: CollectionUtils.atMostOneElement(RoddyBamFile.findAllByWorkPackage(instance.samplePair.mergingWorkPackage1)),
                sampleType2BamFile: CollectionUtils.atMostOneElement(RoddyBamFile.findAllByWorkPackage(instance.samplePair.mergingWorkPackage2)),
        ])

        return instance
    }

    @SuppressWarnings("LineLength") // suppressed because breaking the line would break the commands
    void "test createScript"() {
        given:
        ConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): "/root", (OtpProperty.PATH_TOOLS): "/tools"])

        Path vbpPath = Paths.get("/root/projectDirName_1/sequencing/whole_genome_sequencing/view-by-pid/pid_1")

        RunYapsaInstance instance = setupData()

        ExecuteRunYapsaJob job = new ExecuteRunYapsaJob([
            snvCallingService: Mock(SnvCallingService) {
                1 * getResultRequiredForRunYapsaAndEnsureIsReadableAndNotEmpty(_) >> {
                    vbpPath.resolve("snv_results/paired/" +
                                    "sample-type-name-1_sample-type-name-1/instance-1/snvs_pid_1_somatic_snvs_conf_${MIN_CONFIDENCE_SCORE}_to_10.vcf"
                    )
                }
            }
        ])

        job.referenceGenomeService = Mock(ReferenceGenomeService) {
            fastaFilePath(_) >> { ReferenceGenome referenceGenome ->
                return new File("/reference/genome.fa")
            }
        }
        job.processingOptionService = new ProcessingOptionService()
        job.fileSystemService = new TestFileSystemService()
        job.fileService = new FileService()
        job.runYapsaService = new RunYapsaService()
        job.runYapsaService.individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> vbpPath
        }

        when:
        String result = job.createScript(instance)
        String expected = """\
            module load
            load r
            load programmVersion\\d+

            runYAPSA.R -i /root/projectDirName_\\d+/sequencing/whole_genome_sequencing/view-by-pid/pid_\\d+/snv_results/paired/sample-type-name-\\d+_sample-type-name-\\d+/instance-\\d+/snvs_pid_\\d+_somatic_snvs_conf_${MIN_CONFIDENCE_SCORE}_to_10.vcf -o /root/projectDirName_\\d+/sequencing/whole_genome_sequencing/view-by-pid/pid_\\d+/mutational_signatures_results/paired/sample-type-name-\\d+_sample-type-name-\\d+/instance-\\d+ -s WGS -r /reference/genome.fa -v

            """.stripIndent()

        then:
        result =~ expected

        cleanup:
        configService.clean()
    }
}
