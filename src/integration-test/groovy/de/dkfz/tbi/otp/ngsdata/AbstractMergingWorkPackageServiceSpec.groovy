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
package de.dkfz.tbi.otp.ngsdata

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*

@Rollback
@Integration
class AbstractMergingWorkPackageServiceSpec extends Specification {


    /**
     * this test, tests the opposite of  "findMergingWorkPackage, with chipSeq, find the correct one"()
     * therefore only Pipelines that can align ChipSeq were chosen
    **/
    void "findMergingWorkPackage, no chipSeq, find the correct one"() {
        given:
        Individual individual1 = DomainFactory.createIndividual()
        Individual individual2 = DomainFactory.createIndividual()
        SeqType seqType1 = DomainFactory.createSeqType()
        SeqType seqType2 = DomainFactory.createSeqType()

        // the three Pipelines that support ChipSeq
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

    // this test, tests the opposite of  "findMergingWorkPackage, without chipSeq, find the correct one"()
    void "findMergingWorkPackage, with chipSeq, find the correct one"() {
        given:
        Individual individual1 = DomainFactory.createIndividual()
        Individual individual2 = DomainFactory.createIndividual()
        SeqType seqType = DomainFactory.createChipSeqType()
        AntibodyTarget antibodyTarget1 = DomainFactory.createAntibodyTarget()
        AntibodyTarget antibodyTarget2 = DomainFactory.createAntibodyTarget()

        // the three Pipelines that support ChipSeq
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
        Map<SampleTypePerProject.Category, AbstractMergingWorkPackage> bamsPerCategory = SampleTypePerProject.Category.values().collectEntries { SampleTypePerProject.Category category ->
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
                SampleTypePerProject.Category.DISEASE,
                SampleTypePerProject.Category.CONTROL,
        ]
    }
}
