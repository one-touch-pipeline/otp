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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

class SamplePairSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
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

    List setUpForPathTests(String analysisName) {
        Realm realm = DomainFactory.createRealm()
        Project project = DomainFactory.createProject(
                realm: realm,
        )
        Individual individual = DomainFactory.createIndividual(
                project: project,
        )
        SeqType seqType = DomainFactory.createSeqType(
                libraryLayout: LibraryLayout.PAIRED
        )
        SampleType sampleType1 = DomainFactory.createSampleType(
                name: "tumor",
        )
        SampleType sampleType2 = DomainFactory.createSampleType(
                name: "control",
        )
        DomainFactory.createSampleTypePerProject(
                project: project,
                sampleType: sampleType1,
                category: SampleType.Category.DISEASE,
        )
        MergingWorkPackage mergingWorkPackage1 = DomainFactory.createMergingWorkPackage(
                seqType: seqType,
                sample: DomainFactory.createSample(
                        individual: individual,
                        sampleType: sampleType1,
                ),
        )
        SamplePair samplePair = DomainFactory.createSamplePair(mergingWorkPackage1,
                DomainFactory.createMergingWorkPackage(mergingWorkPackage1, sampleType2))

        return [
                "${project.dirName}/sequencing/${seqType.dirName}/view-by-pid/${individual.pid}/${analysisName.toLowerCase()}_results/paired/tumor_control",
                samplePair,
                project,
        ]
    }

    @Unroll
    void "get #analysisName sample pair path"() {
        given:
        def (String path, SamplePair samplePair, Project project) = setUpForPathTests(analysisName)
        File expectedExtension = new File(path)

        when:
        OtpPath samplePairPath = samplePair."get${analysisName}SamplePairPath"()

        then:
        expectedExtension == samplePairPath.relativePath
        project == samplePairPath.project

        where:
        analysisName << [
                "Indel",
                "Snv",
        ]
    }

    @Unroll
    void "check different mergingWorkPackage classes: #classMWP1, #classMWP2"() {
        given:
        AbstractMergingWorkPackage mergingWorkPackage1 = DomainFactory.createMergingWorkPackage(classMWP1)
        AbstractMergingWorkPackage mergingWorkPackage2 = DomainFactory.createMergingWorkPackage(classMWP1, [
                seqType: mergingWorkPackage1.seqType,
                sample : DomainFactory.createSample([individual: mergingWorkPackage1.individual]),
        ])
        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(mergingWorkPackage1)

        expect:
        DomainFactory.createSamplePair(
                mergingWorkPackage1: mergingWorkPackage1,
                mergingWorkPackage2: mergingWorkPackage2,
        )

        where:
        classMWP1                  | classMWP2
        MergingWorkPackage         | MergingWorkPackage
        ExternalMergingWorkPackage | MergingWorkPackage
        MergingWorkPackage         | ExternalMergingWorkPackage
        ExternalMergingWorkPackage | ExternalMergingWorkPackage
    }
}
