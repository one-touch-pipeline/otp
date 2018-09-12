package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import spock.lang.*

class AbstractMergingWorkPackageServiceSpec extends Specification {


    void "findMergingWorkPackage, no chipSeq, find the correct one"() {
        given:
        Individual individual1 = DomainFactory.createIndividual()
        Individual individual2 = DomainFactory.createIndividual()
        SeqType seqType1 = DomainFactory.createSeqType()
        SeqType seqType2 = DomainFactory.createSeqType()

        [
                Pipeline.Name.PANCAN_ALIGNMENT,
                Pipeline.Name.DEFAULT_OTP,
                Pipeline.Name.EXTERNALLY_PROCESSED,
        ].each { Pipeline.Name pipelineName ->
            [seqType1, seqType2].each { SeqType seqType ->
                [individual1, individual2].each { Individual individual ->
                    DomainFactory.createMergingWorkPackageForPipeline(pipelineName, [
                            seqType: seqType,
                            sample : DomainFactory.createSample([
                                    individual: individual,
                            ])
                    ])
                }
            }
        }

        AbstractMergingWorkPackageService service = new AbstractMergingWorkPackageService()

        when:
        List<AbstractMergingWorkPackage> mergingWorkPackages = service.findMergingWorkPackage(individual1, seqType1)

        then:
        mergingWorkPackages.size() == 3
        mergingWorkPackages*.individual.unique() == [individual1]
        mergingWorkPackages*.seqType.unique() == [seqType1]
        mergingWorkPackages*.pipeline.unique().size() == 3
    }

    void "findMergingWorkPackage, with chipSeq, find the correct one"() {
        given:
        Individual individual1 = DomainFactory.createIndividual()
        Individual individual2 = DomainFactory.createIndividual()
        SeqType seqType = DomainFactory.createChipSeqType()
        AntibodyTarget antibodyTarget1 = DomainFactory.createAntibodyTarget()
        AntibodyTarget antibodyTarget2 = DomainFactory.createAntibodyTarget()

        [
                Pipeline.Name.PANCAN_ALIGNMENT,
                Pipeline.Name.DEFAULT_OTP,
                Pipeline.Name.EXTERNALLY_PROCESSED,
        ].each { Pipeline.Name pipelineName ->
            [individual1, individual2].each { Individual individual ->
                [antibodyTarget1, antibodyTarget2].each { AntibodyTarget antibodyTarget ->
                    DomainFactory.createMergingWorkPackageForPipeline(pipelineName, [
                            seqType       : seqType,
                            sample        : DomainFactory.createSample([
                                    individual: individual,
                            ]),
                            antibodyTarget: antibodyTarget,
                    ])
                }
            }
        }

        AbstractMergingWorkPackageService service = new AbstractMergingWorkPackageService()

        when:
        List<AbstractMergingWorkPackage> mergingWorkPackages = service.findMergingWorkPackage(individual1, seqType, antibodyTarget1)

        then:
        mergingWorkPackages.size() == 3
        mergingWorkPackages*.individual.unique() == [individual1]
        mergingWorkPackages*.seqType.unique() == [seqType]
        mergingWorkPackages*.antibodyTarget.unique() == [antibodyTarget1]
        mergingWorkPackages*.pipeline.unique().size() == 3
    }

    @Unroll
    void "filterByCategory, if a list is given, return only the AbstractMergingWorkPackage for the Category #categoryToFilter"() {
        given:
        Map<SampleType.Category, AbstractMergingWorkPackage> bamsPerCategory = SampleType.Category.values().collectEntries { SampleType.Category category ->
            SampleTypePerProject sampleTypePerProject = DomainFactory.createSampleTypePerProject([
                    category  : category,
            ])
            List<MergingWorkPackage> mergingWorkPackages = [
                    new MergingWorkPackage(
                            sample: new Sample([
                                    individual: new Individual(
                                            project: sampleTypePerProject.project
                                    ),
                                    sampleType: sampleTypePerProject.sampleType,
                            ]),
                    ),
                    new ExternalMergingWorkPackage(
                            sample: new Sample([
                                    individual: new Individual(
                                            project: sampleTypePerProject.project
                                    ),
                                    sampleType: sampleTypePerProject.sampleType,
                            ]),
                    ),
            ]
            [(category): mergingWorkPackages]
        }

        List<AbstractMergingWorkPackage> all = bamsPerCategory.values().flatten()

        AbstractMergingWorkPackageService service = new AbstractMergingWorkPackageService()

        when:
        List<AbstractMergingWorkPackage> mergingWorkPackages = service.filterByCategory(all, categoryToFilter)

        then:
        TestCase.assertContainSame(bamsPerCategory[categoryToFilter], mergingWorkPackages)

        where:
        categoryToFilter << [
                SampleType.Category.DISEASE,
                SampleType.Category.CONTROL,
        ]
    }


    void "filterBySequencingPlatformGroupIfAvailable, if a list is given, return only the MergingWorkPackage with the given seqPlatformGroup and all createExternalMergingWorkPackage"() {
        given:
        SeqPlatformGroup seqPlatformGroup = new SeqPlatformGroup()
        List<AbstractMergingWorkPackage> expectedMergingWorkPackages = [
                new MergingWorkPackage([seqPlatformGroup: seqPlatformGroup]),
                new ExternalMergingWorkPackage(),
                new MergingWorkPackage([seqPlatformGroup: seqPlatformGroup]),
                new ExternalMergingWorkPackage(),
                new MergingWorkPackage([seqPlatformGroup: seqPlatformGroup]),
        ]

        List<AbstractMergingWorkPackage> all = [
                new MergingWorkPackage([seqPlatformGroup: new SeqPlatformGroup()]),
                new MergingWorkPackage([seqPlatformGroup: new SeqPlatformGroup()]),
        ] + expectedMergingWorkPackages

        AbstractMergingWorkPackageService service = new AbstractMergingWorkPackageService()

        when:
        List<AbstractMergingWorkPackage> mergingWorkPackages = service.filterBySequencingPlatformGroupIfAvailable(all, seqPlatformGroup)

        then:
        TestCase.assertContainSame(expectedMergingWorkPackages, mergingWorkPackages)
    }
}
