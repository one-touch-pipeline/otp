package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.dataInstallation.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.spock.*
import spock.lang.Unroll


class ClusterJobSubmissionOptionsServiceIntegrationSpec extends IntegrationSpec {

    @Unroll
    void "test readOptionsFromDatabase ('#realmOption', ''#jobSpecificOption', '#jobAndSeqTypeSpecific')"() {
        given:
        ClusterJobSubmissionOptionsService service = new ClusterJobSubmissionOptionsService()
        service.processingOptionService = new ProcessingOptionService()

        Realm realm = DomainFactory.createRealm(
                defaultJobSubmissionOptions: realmOption,
        )

        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        ProcessParameterObject ppo = DomainFactory.createSeqTrack(seqType: seqType)
        ProcessingStep processingStep = DomainFactory.createAndSaveProcessingStep(CopyFilesJob.simpleName, ppo)

        service.processingOptionService.createOrUpdate(
                ProcessingOption.OptionName.CLUSTER_SUBMISSIONS_OPTION,
                jobSpecificOption,
                "${processingStep.nonQualifiedJobClass}",
        )
        service.processingOptionService.createOrUpdate(
                ProcessingOption.OptionName.CLUSTER_SUBMISSIONS_OPTION,
                jobAndSeqTypeSpecific,
                "${processingStep.nonQualifiedJobClass}_${seqType.processingOptionName}",
        )

        expect:
        result == service.readOptionsFromDatabase(processingStep, realm)

        where:
        realmOption      | jobSpecificOption   | jobAndSeqTypeSpecific || result
        '{"NODES": "1"}' | '{"WALLTIME": "2"}' | '{"QUEUE": "3"}'      || [(JobSubmissionOption.NODES): "1", (JobSubmissionOption.WALLTIME): "2", (JobSubmissionOption.QUEUE): "3"]
        ''               | ''                  | ''                    || [:]
        ''               | ''                  | '{"NODES": "3"}'      || [(JobSubmissionOption.NODES): "3"]
        ''               | '{"NODES": "2"}'    | ''                    || [(JobSubmissionOption.NODES): "2"]
        ''               | '{"NODES": "2"}'    | '{"NODES": "3"}'      || [(JobSubmissionOption.NODES): "3"]
        '{"NODES": "1"}' | ''                  | ''                    || [(JobSubmissionOption.NODES): "1"]
        '{"NODES": "1"}' | ''                  | '{"NODES": "3"}'      || [(JobSubmissionOption.NODES): "3"]
        '{"NODES": "1"}' | '{"NODES": "2"}'    | ''                    || [(JobSubmissionOption.NODES): "2"]
        '{"NODES": "1"}' | '{"NODES": "2"}'    | '{"NODES": "3"}'      || [(JobSubmissionOption.NODES): "3"]
    }
}
