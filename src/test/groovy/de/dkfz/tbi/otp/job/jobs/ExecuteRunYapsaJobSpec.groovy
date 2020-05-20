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
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.runYapsa.ExecuteRunYapsaJob
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

class ExecuteRunYapsaJobSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
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

    void "test createScript"() {
        given:
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

        ConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): "/root", (OtpProperty.PATH_TOOLS): "/tools"])

        RunYapsaInstance instance = DomainFactory.createRunYapsaInstanceWithRoddyBamFiles()

        DomainFactory.createRoddySnvCallingInstance(instance.samplePair, [
                sampleType1BamFile: RoddyBamFile.findByWorkPackage(instance.samplePair.mergingWorkPackage1),
                sampleType2BamFile: RoddyBamFile.findByWorkPackage(instance.samplePair.mergingWorkPackage2),
        ])

        ExecuteRunYapsaJob job = new ExecuteRunYapsaJob([configService: configService])
        job.referenceGenomeService = Mock(ReferenceGenomeService) {
            fastaFilePath(_) >> { ReferenceGenome referenceGenome ->
                return new File("/reference/genome.fa")
            }
        }
        job.processingOptionService = new ProcessingOptionService()

        when:
        String result = job.createScript(instance)
        String expected = """\
            module load
            load r
            load programmVersion\\d+

            mkdir -p -m 2755 /root/projectDirName_\\d+/sequencing/whole_genome_sequencing/view-by-pid/pid_\\d+/mutational_signatures_results/paired/sample-type-name-\\d+_sample-type-name-\\d+/instance-\\d+

            runYAPSA.R -i /root/projectDirName_\\d+/sequencing/whole_genome_sequencing/view-by-pid/pid_\\d+/snv_results/paired/sample-type-name-\\d+_sample-type-name-\\d+/instance-\\d+/snvs_pid_\\d+_somatic_snvs_conf_8_to_10.vcf -o /root/projectDirName_\\d+/sequencing/whole_genome_sequencing/view-by-pid/pid_\\d+/mutational_signatures_results/paired/sample-type-name-\\d+_sample-type-name-\\d+/instance-\\d+ -s WGS -r /reference/genome.fa -v

            """.stripIndent()

        then:
        result =~ expected

        cleanup:
        configService.clean()
    }
}
