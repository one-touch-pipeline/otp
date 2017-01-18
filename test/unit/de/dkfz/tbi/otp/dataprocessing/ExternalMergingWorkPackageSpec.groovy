package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.ExternalMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.ProjectCategory
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SeqType
import grails.test.mixin.Mock
import spock.lang.Specification

@Mock([
        ExternalMergingWorkPackage,
        Individual,
        Pipeline,
        Project,
        ProjectCategory,
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
