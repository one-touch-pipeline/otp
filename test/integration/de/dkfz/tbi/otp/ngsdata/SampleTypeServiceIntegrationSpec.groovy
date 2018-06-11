package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import spock.lang.*

class SampleTypeServiceIntegrationSpec extends Specification {


    void "findUsedSampleTypesForProject, if project is empty, return nothing"() {
        given:
        SampleTypeService service = new SampleTypeService()

        Project project = DomainFactory.createProject()
        DomainFactory.createAllAnalysableSeqTypes()

        when:
        List ret = service.findUsedSampleTypesForProject(project)

        then:
        ret.empty
    }

    void "findUsedSampleTypesForProject, if project has only seqTracks of not analysable SeqTypes, return an empty list"() {
        given:
        SampleTypeService service = new SampleTypeService()

        DomainFactory.createAllAnalysableSeqTypes()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()

        when:
        List ret = service.findUsedSampleTypesForProject(seqTrack.project)

        then:
        ret.empty
    }

    void "findUsedSampleTypesForProject, if project has a SeqTrack of an analysable SeqType, return the SampleType of this SeqTrack"() {
        given:
        SampleTypeService service = new SampleTypeService()

        List<SeqType> seqTypes = DomainFactory.createAllAnalysableSeqTypes()
        SeqTrack seqTrack = DomainFactory.createSeqTrack([
                seqType: seqTypes.first()
        ])

        when:
        List ret = service.findUsedSampleTypesForProject(seqTrack.project)

        then:
        ret == [seqTrack.sampleType]
    }

    void "findUsedSampleTypesForProject, if project has only AbstractMergingWorkPackage of not analysable SeqTypes, return an empty list"() {
        given:
        SampleTypeService service = new SampleTypeService()

        DomainFactory.createAllAnalysableSeqTypes()
        ExternalMergingWorkPackage externalMergingWorkPackage = DomainFactory.createExternalMergingWorkPackage()

        when:
        List ret = service.findUsedSampleTypesForProject(externalMergingWorkPackage.project)

        then:
        ret.empty
    }

    void "findUsedSampleTypesForProject, if project has a AbstractMergingWorkPackage of an analysable SeqType, return the SampleType of this AbstractMergingWorkPackage"() {
        given:
        SampleTypeService service = new SampleTypeService()

        List<SeqType> seqTypes = DomainFactory.createAllAnalysableSeqTypes()
        ExternalMergingWorkPackage externalMergingWorkPackage = DomainFactory.createExternalMergingWorkPackage([
                seqType: seqTypes.first()
        ])

        when:
        List ret = service.findUsedSampleTypesForProject(externalMergingWorkPackage.project)

        then:
        ret == [externalMergingWorkPackage.sampleType]
    }


    void "findUsedSampleTypesForProject, if sampleType is used in SeqTrack and AbstractMergingWorkPackage with an analysable SeqTypes, return the sampleType only once"() {
        given:
        SampleTypeService service = new SampleTypeService()

        List<SeqType> seqTypes = DomainFactory.createAllAnalysableSeqTypes()
        SeqTrack seqTrack = DomainFactory.createSeqTrack([
                seqType: seqTypes.first()
        ])
        ExternalMergingWorkPackage externalMergingWorkPackage = DomainFactory.createExternalMergingWorkPackage([
                seqType: seqTypes.first(),
                sample : seqTrack.sample,
        ])

        when:
        List ret = service.findUsedSampleTypesForProject(externalMergingWorkPackage.project)

        then:
        ret == [externalMergingWorkPackage.sampleType]
    }
}
