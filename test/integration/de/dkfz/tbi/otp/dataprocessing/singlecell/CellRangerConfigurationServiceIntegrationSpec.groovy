package de.dkfz.tbi.otp.dataprocessing.singlecell

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.testing.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.*
import grails.test.spock.*
import org.springframework.validation.*

class CellRangerConfigurationServiceIntegrationSpec extends IntegrationSpec implements CellRangerFactory, UserAndRoles {

    CellRangerConfigurationService cellRangerConfigurationService
    Project project
    SeqType seqType
    Individual individual
    SampleType sampleType
    Sample sample
    Sample sample2
    SeqTrack seqTrack

    void setup() {
        createUserAndRoles()

        project = createProject()
        seqType = createSeqType()
        Pipeline pipeline = findOrCreatePipeline()
        createConfig(project: project, seqType: seqType, pipeline: pipeline)

        DomainFactory.createMergingCriteria(project: project, seqType: seqType)

        individual = DomainFactory.createIndividual(project: project)
        sampleType = DomainFactory.createSampleType()
        sample = createSample(individual: individual, sampleType: sampleType)
        seqTrack = DomainFactory.createSeqTrack(seqType: seqType, sample: sample)
        sample.refresh()

        Individual individual2 = DomainFactory.createIndividual(project: project)
        SampleType sampleType2 = DomainFactory.createSampleType()
        sample2 = createSample(individual: individual2, sampleType: sampleType2)
        DomainFactory.createSeqTrack(seqType: seqType, sample: sample2)
        sample2.refresh()
    }

    void "test getSamples"() {
        when:
        CellRangerConfigurationService.Samples samples = SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.getSamples(project, individual, sampleType)
        }

        then:
        samples.allSamples == [sample, sample2]
        samples.selectedSamples == [sample]
    }

    void "test getSamples for whole project "() {
        when:
        CellRangerConfigurationService.Samples samples = SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.getSamples(project, null, null)
        }

        then:
        samples.allSamples == [sample, sample2]
        samples.selectedSamples == [sample, sample2]
    }

    void "test createMergingWorkPackage"() {
        when:
        Errors errors = SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.createMergingWorkPackage(1, 2, project, individual, sampleType)
        }

        then:
        !errors
        CellRangerMergingWorkPackage mwp = CollectionUtils.exactlyOneElement(CellRangerMergingWorkPackage.all)
        mwp.sample == sample
        mwp.seqTracks == [seqTrack] as Set
        mwp.expectedCells == 1
        mwp.enforcedCells == 2
        mwp.project == project
        mwp.seqType == seqType
        mwp.individual == individual
    }

    void "test createMergingWorkPackage for whole project"() {
        when:
        Errors errors = SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.createMergingWorkPackage(1, 2, project, null, null)
        }

        then:
        !errors
        CellRangerMergingWorkPackage.all.size() == 2
        CellRangerMergingWorkPackage.all*.sample as Set == [sample, sample2] as Set
    }
}
