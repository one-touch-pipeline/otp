package workflows.analysis.pair.runyapsa

import de.dkfz.tbi.otp.dataprocessing.ConfigPerProjectAndSeqType
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.ProjectService
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import grails.plugin.springsecurity.*
import org.joda.time.*
import workflows.analysis.pair.AbstractRoddyBamFilePairAnalysisWorkflowTests

abstract class AbstractRunYapsaWorkflowTests extends AbstractRoddyBamFilePairAnalysisWorkflowTests<RunYapsaInstance> {

    LsdfFilesService lsdfFilesService

    ProjectService projectService

    ProcessingOptionService processingOptionService


    @Override
    ConfigPerProjectAndSeqType createConfig() {
        DomainFactory.createProcessingOptionLazy([
                name: ProcessingOption.OptionName.PIPELINE_MIN_COVERAGE,
                type: Pipeline.Type.MUTATIONAL_SIGNATURE.toString(),
                project: null,
                value: "20",
        ])
        DomainFactory.createRunYapsaPipelineLazy()
        DomainFactory.createRunYapsaSeqTypes()
        DomainFactory.createReferenceGenomeProjectSeqType(
                referenceGenome: referenceGenome,
                project: project,
                seqType: seqType,
        )
        lsdfFilesService.createDirectory(configService.getProjectSequencePath(project), realm)

            config= DomainFactory.createRunYapsaConfig(
                    [
                            programVersion: "yapsa-devel/80f748e",
                            project : samplePair.project,
                            seqType : samplePair.seqType,
                            pipeline: Pipeline.Name.RUN_YAPSA.pipeline,
                    ]
            )


    }

    @Override
    ReferenceGenome createReferenceGenome() {
        ReferenceGenome referenceGenome = super.createReferenceGenome()

        SpringSecurityUtils.doWithAuth("operator") {
            processingOptionService.createOrUpdate(
                    OptionName.PIPELINE_RUNYAPSA_REFERENCE_GENOME,
                    null,
                    null,
                    referenceGenome.name
            )
        }

        return referenceGenome
    }


    void createRunYapsaInput() {
        File sourceSnvCallingInputFile

        if (seqType == SeqType.wholeGenomePairedSeqType) {
            sourceSnvCallingInputFile = new File(workflowData, "snvs_stds_somatic_snvs_conf_8_to_10-wgs.vcf")
        } else if (seqType == SeqType.exomePairedSeqType) {
            sourceSnvCallingInputFile = new File(workflowData, "snvs_stds_somatic_snvs_conf_8_to_10-wes.vcf")
        } else {
            throw new Exception("The SeqType '${seqType}' is not supported by runYapsa workflow")
        }

        RoddySnvCallingInstance snvCallingInstance = DomainFactory.createRoddySnvCallingInstance(samplePair)
        File runYapsaInputFile = snvCallingInstance.getResultRequiredForRunYapsa()
        samplePair.snvProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        assert samplePair.save(flush: true)

        linkFileUtils.createAndValidateLinks([
                (sourceSnvCallingInputFile): runYapsaInputFile,
        ], realm)
    }


    void executeTest() {
        createRunYapsaInput()

        super.executeTest()
    }


    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/RunYapsaWorkflow.groovy",
                "scripts/initializations/RunYapsaPipelineOptions.groovy",
                "scripts/initializations/LoadSoftwareModules.groovy",
        ]
    }

    List<File> filesToCheck(RunYapsaInstance runYapsaInstance) {
        return []
    }


    @Override
    File getWorkflowData() {
        new File(getDataDirectory(), 'runYapsa')
    }

    @Override
    Duration getTimeout() {
        Duration.standardHours(24)
    }
}
