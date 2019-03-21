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


import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

class ReferenceGenomeProjectSeqTypeSpec extends Specification implements DataTest {

    Class[] getDomainClassesToMock() {[
            Project,
            ProjectCategory,
            Individual,
            Realm,
            Run,
            SeqTrack,
            Sample,
            SampleType,
            SeqCenter,
            SeqPlatform,
            SeqPlatformGroup,
            SeqPlatformModelLabel,
            SeqType,
            SoftwareTool,
            ReferenceGenome,
            ReferenceGenomeProjectSeqType,
    ]}

    static final String PROJECT_NAME = "project"
    static final String SEQ_TYPE_NAME = "seqType"
    static final String PROJECT_DEFAULT_SAMPLE_TYPE_NAME = "projectDefaultSampleType"
    static final String SAMPLE_TYPE_SPECIFIC_SAMPLE_TYPE_NAME = "sampleTypeSpecificSampleType"

    static final String OTHER_PROJECT_NAME = "otherProject"
    static final String OTHER_SEQ_TYPE_NAME = "otherSeqType"
    static final String OTHER_SAMPLE_TYPE_NAME = "otherSampleType"


    @Unroll
    void "test getConfiguredReferenceGenomeProjectSeqType with #projectName #seqTypeName #sampleTypeName"() {
        given:
        Project project = DomainFactory.createProject(name: PROJECT_NAME)
        SeqType seqType = DomainFactory.createSeqType(name: SEQ_TYPE_NAME)
        DomainFactory.createSampleType(
                name: PROJECT_DEFAULT_SAMPLE_TYPE_NAME,
                specificReferenceGenome: SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        )
        SampleType sampleTypeSpecificSampleType = DomainFactory.createSampleType(
                name: SAMPLE_TYPE_SPECIFIC_SAMPLE_TYPE_NAME,
                specificReferenceGenome: SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        )
        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()

        DomainFactory.createReferenceGenomeProjectSeqType([
                referenceGenome: referenceGenome,
                project        : project,
                seqType        : seqType,
        ])
        DomainFactory.createReferenceGenomeProjectSeqType([
                referenceGenome: referenceGenome,
                project        : project,
                seqType        : seqType,
                sampleType     : sampleTypeSpecificSampleType,
        ])

        DomainFactory.createProject(name: OTHER_PROJECT_NAME)
        DomainFactory.createSeqType(name: OTHER_SEQ_TYPE_NAME)
        DomainFactory.createSampleType(
                name: OTHER_SAMPLE_TYPE_NAME,
                specificReferenceGenome: SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC,
        )

        when:
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType =
                ReferenceGenomeProjectSeqType.getConfiguredReferenceGenomeProjectSeqType(
                        Project.findByName(projectName),
                        SeqType.findByName(seqTypeName),
                        SampleType.findByName(sampleTypeName)
                )

        then:
        expectValue == (referenceGenomeProjectSeqType != null)

        where:
        projectName        | seqTypeName         | sampleTypeName                        || expectValue
        PROJECT_NAME       | SEQ_TYPE_NAME       | PROJECT_DEFAULT_SAMPLE_TYPE_NAME      || true
        PROJECT_NAME       | SEQ_TYPE_NAME       | SAMPLE_TYPE_SPECIFIC_SAMPLE_TYPE_NAME || true

        OTHER_PROJECT_NAME | SEQ_TYPE_NAME       | PROJECT_DEFAULT_SAMPLE_TYPE_NAME      || false
        OTHER_PROJECT_NAME | SEQ_TYPE_NAME       | SAMPLE_TYPE_SPECIFIC_SAMPLE_TYPE_NAME || false
        PROJECT_NAME       | OTHER_SEQ_TYPE_NAME | PROJECT_DEFAULT_SAMPLE_TYPE_NAME      || false
        PROJECT_NAME       | OTHER_SEQ_TYPE_NAME | SAMPLE_TYPE_SPECIFIC_SAMPLE_TYPE_NAME || false
        PROJECT_NAME       | SEQ_TYPE_NAME       | OTHER_SAMPLE_TYPE_NAME                || false
    }

    void "test getConfiguredReferenceGenomeProjectSeqType for seqTrack"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()

        DomainFactory.createReferenceGenomeProjectSeqType([
                referenceGenome: referenceGenome,
                project        : seqTrack.project,
                seqType        : seqTrack.seqType,
        ])

        when:
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType =
                ReferenceGenomeProjectSeqType.getConfiguredReferenceGenomeProjectSeqType(
                        seqTrack,
                )

        then:
        null != referenceGenomeProjectSeqType
    }

    void "test getConfiguredReferenceGenomeProjectSeqType should fail for project is null"() {
        given:
        SeqType seqType = DomainFactory.createSeqType(name: SEQ_TYPE_NAME)
        SampleType sampleType = DomainFactory.createSampleType(
                name: PROJECT_DEFAULT_SAMPLE_TYPE_NAME,
                specificReferenceGenome: SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        )

        when:
        ReferenceGenomeProjectSeqType.getConfiguredReferenceGenomeProjectSeqType(
                null,
                seqType,
                sampleType
        )

        then:
        AssertionError e = thrown()
        e.message.contains('project')
    }

    void "test getConfiguredReferenceGenomeProjectSeqType should fail for seqType is null"() {
        given:
        Project project = DomainFactory.createProject(name: PROJECT_NAME)
        SampleType sampleType = DomainFactory.createSampleType(
                name: PROJECT_DEFAULT_SAMPLE_TYPE_NAME,
                specificReferenceGenome: SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        )

        when:
        ReferenceGenomeProjectSeqType.getConfiguredReferenceGenomeProjectSeqType(
                project,
                null,
                sampleType
        )

        then:
        AssertionError e = thrown()
        e.message.contains('seqType')
    }

    void "test getConfiguredReferenceGenomeProjectSeqType should fail for sampleType is null"() {
        given:
        Project project = DomainFactory.createProject(name: PROJECT_NAME)
        SeqType seqType = DomainFactory.createSeqType(name: SEQ_TYPE_NAME)

        when:
        ReferenceGenomeProjectSeqType.getConfiguredReferenceGenomeProjectSeqType(
                project,
                seqType,
                null
        )

        then:
        AssertionError e = thrown()
        e.message.contains('sampleType')
    }


    void "test getConfiguredReferenceGenomeProjectSeqType should fail for sampleType.specificReferenceGenome is unknown"() {
        given:
        Project project = DomainFactory.createProject(name: PROJECT_NAME)
        SeqType seqType = DomainFactory.createSeqType(name: SEQ_TYPE_NAME)
        SampleType sampleType = DomainFactory.createSampleType(
                name: OTHER_SAMPLE_TYPE_NAME,
                specificReferenceGenome: SampleType.SpecificReferenceGenome.UNKNOWN
        )

        when:
        ReferenceGenomeProjectSeqType.getConfiguredReferenceGenomeProjectSeqType(
                project,
                seqType,
                sampleType,
        )

        then:
        RuntimeException e = thrown()
        e.message.contains('the way to fetch the reference genome is not defined')
    }
}
