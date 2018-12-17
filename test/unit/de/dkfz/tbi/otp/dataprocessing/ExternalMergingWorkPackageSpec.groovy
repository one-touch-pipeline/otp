package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*

@Mock([
        ExternalMergingWorkPackage,
        Individual,
        Pipeline,
        Project,
        ProjectCategory,
        Realm,
        ReferenceGenome,
        Sample,
        SampleType,
        SeqType,
])
class ExternalMergingWorkPackageSpec extends Specification {

    void "test constraint, when all fine then no exception should be thrown"() {
        given:
        ExternalMergingWorkPackage externalMergingWorkPackage = DomainFactory.createExternalMergingWorkPackage()

        expect:
        externalMergingWorkPackage.pipeline.name == Pipeline.Name.EXTERNALLY_PROCESSED
    }
}
