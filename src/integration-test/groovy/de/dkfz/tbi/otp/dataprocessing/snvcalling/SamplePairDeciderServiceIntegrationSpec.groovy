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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import grails.util.Pair
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@Rollback
@Integration
class SamplePairDeciderServiceIntegrationSpec extends Specification implements IsRoddy {

    private Map<String, ?> createDataForIndividual(Project project, SeqType seqType) {
        Individual individual = createIndividual([project: project])
        Sample sampleDisease1 = createSample([individual: individual])
        Sample sampleDisease2 = createSample([individual: individual])
        Sample sampleControl1 = createSample([individual: individual])
        Sample sampleControl2 = createSample([individual: individual])

        MergingWorkPackage mwpDisease1 = createMergingWorkPackage([
                seqType: seqType,
                sample : sampleDisease1,
        ])
        MergingWorkPackage mwpDisease2 = createMergingWorkPackage([
                seqType: seqType,
                sample : sampleDisease2,
        ])
        MergingWorkPackage mwpControl1 = createMergingWorkPackage([
                seqType: seqType,
                sample : sampleControl1,
        ])
        MergingWorkPackage mwpControl2 = createMergingWorkPackage([
                seqType: seqType,
                sample : sampleControl2,
        ])

        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(mwpDisease1, SampleTypePerProject.Category.DISEASE)
        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(mwpDisease2, SampleTypePerProject.Category.DISEASE)
        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(mwpControl1, SampleTypePerProject.Category.CONTROL)
        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(mwpControl2, SampleTypePerProject.Category.CONTROL)

        return [
                individual         : individual,
                allWorkPackages    : [
                        mwpDisease1,
                        mwpDisease2,
                        mwpControl1,
                        mwpControl2,
                ],
                diseaseWorkPackages: [
                        mwpDisease1,
                        mwpDisease2,
                ],
                controlWorkPackages: [
                        mwpControl1,
                        mwpControl2,
                ],
                pairs              : [
                        new Pair<AbstractMergingWorkPackage, AbstractMergingWorkPackage>(mwpDisease1, mwpControl1),
                        new Pair<AbstractMergingWorkPackage, AbstractMergingWorkPackage>(mwpDisease1, mwpControl2),
                        new Pair<AbstractMergingWorkPackage, AbstractMergingWorkPackage>(mwpDisease2, mwpControl1),
                        new Pair<AbstractMergingWorkPackage, AbstractMergingWorkPackage>(mwpDisease2, mwpControl2),
                ],
        ]
    }

    void "findOrCreateSamplePairsForProject(project), if multiple samples available, create correct sample pairs"() {
        given:
        Project project1 = createProject()
        SeqType seqType = DomainFactory.createAllAnalysableSeqTypes().first()
        Map<String, ?> data1a = createDataForIndividual(project1, seqType)
        Map<String, ?> data1b = createDataForIndividual(project1, seqType)
        Map<String, ?> data1c = createDataForIndividual(project1, seqType)
        createDataForIndividual(createProject(), seqType)
        createDataForIndividual(createProject(), seqType)

        List<Pair<AbstractMergingWorkPackage, AbstractMergingWorkPackage>> expected = [
                data1a,
                data1b,
                data1c,
        ].collectMany {
            it.pairs
        }

        SamplePairDeciderService service = new SamplePairDeciderService([
                abstractMergingWorkPackageService: Mock(AbstractMergingWorkPackageService) {
                    4 * findMergingWorkPackage(data1a.individual, _, _) >> data1a.allWorkPackages
                    4 * findMergingWorkPackage(data1b.individual, _, _) >> data1b.allWorkPackages
                    4 * findMergingWorkPackage(data1c.individual, _, _) >> data1c.allWorkPackages
                    2 * filterByCategory(data1a.allWorkPackages, SampleTypePerProject.Category.CONTROL) >> data1a.controlWorkPackages
                    2 * filterByCategory(data1b.allWorkPackages, SampleTypePerProject.Category.CONTROL) >> data1b.controlWorkPackages
                    2 * filterByCategory(data1c.allWorkPackages, SampleTypePerProject.Category.CONTROL) >> data1c.controlWorkPackages
                    2 * filterByCategory(data1a.allWorkPackages, SampleTypePerProject.Category.DISEASE) >> data1a.diseaseWorkPackages
                    2 * filterByCategory(data1b.allWorkPackages, SampleTypePerProject.Category.DISEASE) >> data1b.diseaseWorkPackages
                    2 * filterByCategory(data1c.allWorkPackages, SampleTypePerProject.Category.DISEASE) >> data1c.diseaseWorkPackages
                    0 * _
                }
        ])

        when:
        List<SamplePair> samplePairs = service.findOrCreateSamplePairsForProject(project1)

        then:
        samplePairs.size() == 3 * 4
        TestCase.assertContainSame(samplePairs.collect {
            new Pair<AbstractMergingWorkPackage, AbstractMergingWorkPackage>(it.mergingWorkPackage1, it.mergingWorkPackage2)
        }, expected)
    }
}
