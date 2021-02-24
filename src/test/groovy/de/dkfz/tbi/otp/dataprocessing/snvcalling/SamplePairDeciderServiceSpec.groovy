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
package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

class SamplePairDeciderServiceSpec extends Specification implements DataTest, DomainFactoryCore, IsRoddy {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractMergingWorkPackage,
                ExternalMergingWorkPackage,
                Individual,
                LibraryPreparationKit,
                MergingWorkPackage,
                Pipeline,
                Project,
                Realm,
                ReferenceGenome,
                Sample,
                SamplePair,
                SampleType,
                SampleTypePerProject,
                SeqPlatformGroup,
                SeqType,
        ]
    }

    @Unroll
    void "findOrCreateSamplePairs(mergingWorkPackage), create SamplePair for #pipelineName1 and #pipelineName2 and #category1"() {
        given:
        DomainFactory.createAllAnalysableSeqTypes()
        AbstractMergingWorkPackage mergingWorkPackage1 = DomainFactory.createMergingWorkPackageForPipeline(pipelineName1, [
                seqType: SeqTypeService.wholeGenomePairedSeqType,
        ])
        AbstractMergingWorkPackage mergingWorkPackage2 = DomainFactory.createMergingWorkPackageForPipeline(pipelineName2, [
                seqType: mergingWorkPackage1.seqType,
                sample : createSample([individual: mergingWorkPackage1.individual]),
        ])
        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(mergingWorkPackage1, category1)
        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(mergingWorkPackage2, category1.correspondingCategory())

        SamplePairDeciderService service = new SamplePairDeciderService([
                abstractMergingWorkPackageService: Mock(AbstractMergingWorkPackageService) {
                    1 * findMergingWorkPackage(_, _, _) >> [mergingWorkPackage1, mergingWorkPackage2]
                    1 * filterByCategory(_, _) >> [mergingWorkPackage2]
                }
        ])

        when:
        List<SamplePair> samplePairs = service.findOrCreateSamplePairs(mergingWorkPackage1)

        then:
        SamplePair samplePair = CollectionUtils.exactlyOneElement(samplePairs)
        if (category1 == SampleType.Category.DISEASE) {
            assert samplePair.mergingWorkPackage1 == mergingWorkPackage1
            assert samplePair.mergingWorkPackage2 == mergingWorkPackage2
        } else {
            assert samplePair.mergingWorkPackage1 == mergingWorkPackage2
            assert samplePair.mergingWorkPackage2 == mergingWorkPackage1
        }

        where:
        pipelineName1                      | pipelineName2                      | category1
        Pipeline.Name.PANCAN_ALIGNMENT     | Pipeline.Name.PANCAN_ALIGNMENT     | SampleType.Category.DISEASE
        Pipeline.Name.PANCAN_ALIGNMENT     | Pipeline.Name.EXTERNALLY_PROCESSED | SampleType.Category.DISEASE
        Pipeline.Name.DEFAULT_OTP          | Pipeline.Name.DEFAULT_OTP          | SampleType.Category.DISEASE
        Pipeline.Name.DEFAULT_OTP          | Pipeline.Name.EXTERNALLY_PROCESSED | SampleType.Category.DISEASE
        Pipeline.Name.EXTERNALLY_PROCESSED | Pipeline.Name.EXTERNALLY_PROCESSED | SampleType.Category.DISEASE
        Pipeline.Name.EXTERNALLY_PROCESSED | Pipeline.Name.PANCAN_ALIGNMENT     | SampleType.Category.DISEASE
        Pipeline.Name.EXTERNALLY_PROCESSED | Pipeline.Name.DEFAULT_OTP          | SampleType.Category.DISEASE
        Pipeline.Name.PANCAN_ALIGNMENT     | Pipeline.Name.PANCAN_ALIGNMENT     | SampleType.Category.CONTROL
        Pipeline.Name.PANCAN_ALIGNMENT     | Pipeline.Name.EXTERNALLY_PROCESSED | SampleType.Category.CONTROL
        Pipeline.Name.DEFAULT_OTP          | Pipeline.Name.DEFAULT_OTP          | SampleType.Category.CONTROL
        Pipeline.Name.DEFAULT_OTP          | Pipeline.Name.EXTERNALLY_PROCESSED | SampleType.Category.CONTROL
        Pipeline.Name.EXTERNALLY_PROCESSED | Pipeline.Name.EXTERNALLY_PROCESSED | SampleType.Category.CONTROL
        Pipeline.Name.EXTERNALLY_PROCESSED | Pipeline.Name.PANCAN_ALIGNMENT     | SampleType.Category.CONTROL
        Pipeline.Name.EXTERNALLY_PROCESSED | Pipeline.Name.DEFAULT_OTP          | SampleType.Category.CONTROL

        filterSeqPlatformGroup = pipelineName1 == Pipeline.Name.EXTERNALLY_PROCESSED ? 0 : 1
    }

    void "findOrCreateSamplePairs(mergingWorkPackage), if multiple samples available, create correct sample pairs"() {
        given:
        SeqType seqType = DomainFactory.createAllAnalysableSeqTypes().first()
        Individual individual = createIndividual()

        List<AbstractMergingWorkPackage> mergingWorkPackages = (0..6).collect {
            DomainFactory.createMergingWorkPackage([
                    seqType: seqType,
                    sample : createSample([individual: individual]),
            ])
        }
        mergingWorkPackages[0..1].each {
            DomainFactory.createSampleTypePerProjectForMergingWorkPackage(it, SampleType.Category.DISEASE)
        }
        mergingWorkPackages[2..4].each {
            DomainFactory.createSampleTypePerProjectForMergingWorkPackage(it, SampleType.Category.CONTROL)
        }
        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(mergingWorkPackages[5], SampleType.Category.IGNORED)
        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(mergingWorkPackages[6], SampleType.Category.UNDEFINED)

        SamplePairDeciderService service = new SamplePairDeciderService([
                abstractMergingWorkPackageService: Mock(AbstractMergingWorkPackageService) {
                    1 * findMergingWorkPackage(_, _, _) >> mergingWorkPackages
                    1 * filterByCategory(_, _) >> mergingWorkPackages[2..4]
                }
        ])

        when:
        List<SamplePair> samplePairs = service.findOrCreateSamplePairs(mergingWorkPackages[0])

        then:
        samplePairs.size() == 3
        samplePairs*.mergingWorkPackage1.unique() == [mergingWorkPackages[0]]
        TestCase.assertContainSame(samplePairs*.mergingWorkPackage2, mergingWorkPackages[2..4])
    }

    void "findOrCreateSamplePairs(mergingWorkPackage), if SeqType is not analysable, return empty list"() {
        given:
        Project project1 = createProject()
        SeqType seqType = DomainFactory.createRnaSingleSeqType()
        DomainFactory.createAllAnalysableSeqTypes()
        Individual individual = createIndividual([project: project1])
        Sample sampleDisease1 = createSample([individual: individual])
        Sample sampleControl1 = createSample([individual: individual])

        MergingWorkPackage mwpDisease1 = createMergingWorkPackage([
                seqType: seqType,
                sample : sampleDisease1,
        ])

        MergingWorkPackage mwpControl1 = createMergingWorkPackage([
                seqType: seqType,
                sample : sampleControl1,
        ])

        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(mwpDisease1, SampleType.Category.DISEASE)
        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(mwpControl1, SampleType.Category.CONTROL)

        SamplePairDeciderService service = new SamplePairDeciderService([
                abstractMergingWorkPackageService: Mock(AbstractMergingWorkPackageService) {
                    0 * findMergingWorkPackage(_, _, _)
                }
        ])

        when:
        List<SamplePair> samplePairs = service.findOrCreateSamplePairs(mwpDisease1)
        List<SamplePair> samplePairs2 = service.findOrCreateSamplePairs(mwpControl1)

        then:
        samplePairs.empty
        samplePairs2.empty
    }

    void "findOrCreateSamplePairs(mergingWorkPackage), if category is not defined, return empty list"() {
        given:
        AbstractMergingWorkPackage mergingWorkPackage = new MergingWorkPackage([
                seqType: DomainFactory.createAllAnalysableSeqTypes().first(),
                sample : new Sample([
                        individual: new Individual([
                                project: createProject(),
                        ]),
                        sampleType: createSampleType(),
                ]),
        ])

        SamplePairDeciderService service = new SamplePairDeciderService([
                abstractMergingWorkPackageService: Mock(AbstractMergingWorkPackageService) {
                    0 * findMergingWorkPackage(_, _, _)
                }
        ])

        when:
        List<SamplePair> samplePairs = service.findOrCreateSamplePairs(mergingWorkPackage)

        then:
        samplePairs.empty
    }

    @Unroll
    void "findOrCreateSamplePairs(mergingWorkPackage), if category is #category, return empty list"() {
        given:
        SampleTypePerProject sampleTypePerProject = DomainFactory.createSampleTypePerProject([
                category: category,
        ])
        AbstractMergingWorkPackage mergingWorkPackage = new MergingWorkPackage([
                seqType: DomainFactory.createAllAnalysableSeqTypes().first(),
                sample : new Sample([
                        individual: new Individual([
                                project: sampleTypePerProject.project,
                        ]),
                        sampleType: sampleTypePerProject.sampleType,
                ]),
        ])

        SamplePairDeciderService service = new SamplePairDeciderService([
                abstractMergingWorkPackageService: Mock(AbstractMergingWorkPackageService) {
                    0 * findMergingWorkPackage(_, _, _)
                }
        ])

        when:
        List<SamplePair> samplePairs = service.findOrCreateSamplePairs(mergingWorkPackage)

        then:
        samplePairs.empty

        where:
        category << [
                SampleType.Category.IGNORED,
                SampleType.Category.UNDEFINED,
        ]
    }

    void "findOrCreateSamplePairs(mergingWorkPackage), mergingWorkPackage is null, then throw assertion"() {
        given:
        SamplePairDeciderService service = new SamplePairDeciderService()

        when:
        service.findOrCreateSamplePairs(null as MergingWorkPackage)

        then:
        thrown(AssertionError)
    }

    void "findOrCreateSamplePairs(mergingWorkPackages), create correct sample pairs for all given MergingWorkPackages"() {
        given:
        SeqType seqType = DomainFactory.createAllAnalysableSeqTypes().first()
        Individual individual = createIndividual()

        List<AbstractMergingWorkPackage> mergingWorkPackages = (0..9).collect {
            DomainFactory.createMergingWorkPackage([
                    seqType: seqType,
                    sample : createSample([individual: individual]),
            ])
        }
        mergingWorkPackages[0..3].each {
            DomainFactory.createSampleTypePerProjectForMergingWorkPackage(it, SampleType.Category.DISEASE)
        }
        mergingWorkPackages[4..7].each {
            DomainFactory.createSampleTypePerProjectForMergingWorkPackage(it, SampleType.Category.CONTROL)
        }
        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(mergingWorkPackages[8], SampleType.Category.IGNORED)
        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(mergingWorkPackages[9], SampleType.Category.UNDEFINED)

        SamplePairDeciderService service = new SamplePairDeciderService([
                abstractMergingWorkPackageService: Mock(AbstractMergingWorkPackageService) {
                    4 * findMergingWorkPackage(_, _, _) >> mergingWorkPackages
                    2 * filterByCategory(_, SampleType.Category.DISEASE) >> mergingWorkPackages[0..3]
                    2 * filterByCategory(_, SampleType.Category.CONTROL) >> mergingWorkPackages[4..7]
                }
        ])
        List expectedCombination = [
                [mergingWorkPackages[0], mergingWorkPackages[4]],
                [mergingWorkPackages[0], mergingWorkPackages[5]],
                [mergingWorkPackages[0], mergingWorkPackages[6]],
                [mergingWorkPackages[0], mergingWorkPackages[7]],

                [mergingWorkPackages[1], mergingWorkPackages[4]],
                [mergingWorkPackages[1], mergingWorkPackages[5]],
                [mergingWorkPackages[1], mergingWorkPackages[6]],
                [mergingWorkPackages[1], mergingWorkPackages[7]],

                [mergingWorkPackages[2], mergingWorkPackages[6]],
                [mergingWorkPackages[2], mergingWorkPackages[7]],
                [mergingWorkPackages[3], mergingWorkPackages[6]],
                [mergingWorkPackages[3], mergingWorkPackages[7]],
        ].sort()

        when:
        List<AbstractMergingWorkPackage> mergingWorkPackageList = mergingWorkPackages[0, 1, 6, 7, 8, 9]
        List<SamplePair> samplePairs = service.findOrCreateSamplePairs(mergingWorkPackageList)

        then:
        samplePairs.size() == 12
        List foundCombination = samplePairs.collect {
            [it.mergingWorkPackage1, it.mergingWorkPackage2]
        }.sort()
        foundCombination == expectedCombination
    }

    @Unroll
    void "findOrCreateSamplePairs(mergingWorkPackages), if list is empty, return empty list"() {
        given:
        SamplePairDeciderService service = new SamplePairDeciderService()

        when:
        List<SamplePair> samplePairs = service.findOrCreateSamplePairs([])

        then:
        samplePairs.size() == 0
    }

    void "findOrCreateSamplePairs(mergingWorkPackages), if list is null, throw assertion"() {
        given:
        SamplePairDeciderService service = new SamplePairDeciderService()

        when:
        service.findOrCreateSamplePairs(null as List)

        then:
        thrown(AssertionError)
    }

    void "findOrCreateSamplePair(disease,  control), if sample pair does not exist, create new one"() {
        given:
        SamplePairDeciderService service = new SamplePairDeciderService()

        DomainFactory.createAllAnalysableSeqTypes()

        MergingWorkPackage disease = DomainFactory.createMergingWorkPackage()
        MergingWorkPackage control = DomainFactory.createMergingWorkPackage([
                seqType: disease.seqType,
                sample : createSample([
                        individual: disease.individual,
                ]),
        ])

        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(disease, SampleType.Category.DISEASE)
        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(control, SampleType.Category.CONTROL)

        expect:
        SamplePair.count() == 0

        when:
        SamplePair returnedSamplePairs = service.findOrCreateSamplePair(disease, control)

        then:
        returnedSamplePairs
        returnedSamplePairs.mergingWorkPackage1 == disease
        returnedSamplePairs.mergingWorkPackage2 == control
        SamplePair.count() == 1
    }

    void "findOrCreateSamplePair(disease,  control), if sample pair already exist, return existing one"() {
        SamplePairDeciderService service = new SamplePairDeciderService()

        DomainFactory.createAllAnalysableSeqTypes()

        MergingWorkPackage disease = DomainFactory.createMergingWorkPackage()
        MergingWorkPackage control = DomainFactory.createMergingWorkPackage([
                seqType: disease.seqType,
                sample : createSample([
                        individual: disease.individual,
                ]),
        ])

        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(disease, SampleType.Category.DISEASE)
        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(control, SampleType.Category.CONTROL)

        SamplePair samplePair = DomainFactory.createSamplePair([
                mergingWorkPackage1: disease,
                mergingWorkPackage2: control,
        ])

        when:
        SamplePair returnedSamplePairs = service.findOrCreateSamplePair(disease, control)

        then:
        returnedSamplePairs == samplePair
        SamplePair.count() == 1
    }
}
