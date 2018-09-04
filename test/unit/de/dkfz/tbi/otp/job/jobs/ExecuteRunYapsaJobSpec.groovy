package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.runYapsa.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

@Mock([
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
        RunSegment,
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
])
class ExecuteRunYapsaJobSpec extends Specification {

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
                name: COMMAND_ACTIVATION_RUN_YAPSA_PREFIX,
                value: "load",
        )
        DomainFactory.createProcessingOptionLazy(
                name: COMMAND_R,
                value: "r",
        )
        DomainFactory.createProcessingOptionLazy(
                name: COMMAND_RUN_YAPSA,
                value: "yapsa",
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

        when:
        String result = job.createScript(instance)
        String expected = """\
            module load
            load r
            load programmVersion\\d+

            mkdir -p -m 2755 /root/projectDirName_\\d+/sequencing/whole_genome_sequencing/view-by-pid/pid_\\d+/mutational_signatures_results/paired/sampletypename-\\d+_sampletypename-\\d+/instance-\\d+

            r ${configService.getToolsPath()}/yapsa -i /root/projectDirName_\\d+/sequencing/whole_genome_sequencing/view-by-pid/pid_\\d+/snv_results/paired/sampletypename-\\d+_sampletypename-\\d+/instance-\\d+/snvs_pid_\\d+_somatic_snvs_conf_8_to_10.vcf -o /root/projectDirName_\\d+/sequencing/whole_genome_sequencing/view-by-pid/pid_\\d+/mutational_signatures_results/paired/sampletypename-\\d+_sampletypename-\\d+/instance-\\d+ -s WGS -r /reference/genome.fa -v

            """.stripIndent()

        then:
        result =~ expected

        cleanup:
        configService.clean()
    }
}
