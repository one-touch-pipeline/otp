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
import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.TestCase

@Rollback
@Integration
class ReferenceGenomeProjectSeqTypeTests {

    Project project
    SeqType seqType
    SampleType sampleType
    ReferenceGenome referenceGenome

    @Before
    void setup() {
        project = DomainFactory.createProject()
        seqType = DomainFactory.createSeqType()
        referenceGenome = DomainFactory.createReferenceGenome()
        sampleType = null
    }

    private ReferenceGenomeProjectSeqType createReferenceGenomeProjectSeqType(Date deprecatedDate, boolean doSave = true) {
        ReferenceGenomeProjectSeqType domain = new ReferenceGenomeProjectSeqType(
                        project: project,
                        seqType: seqType,
                        referenceGenome: referenceGenome,
                        sampleType: sampleType,
                        deprecatedDate: deprecatedDate)
        if (doSave) {
            domain.save(flush: true)
        }
        return domain
    }

    @Test
    void testUnique_NoDuplication_WithDifferentProjects() {
        createReferenceGenomeProjectSeqType(null)
        project = DomainFactory.createProject()

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        assert newDomain.validate()
    }

    @Test
    void testUnique_NoDuplication_WithDifferentSeqType() {
        createReferenceGenomeProjectSeqType(null)
        seqType = DomainFactory.createSeqType()

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        assert newDomain.validate()
    }

    @Test
    void testUnique_NoDuplication_WithAndWithoutSampleType() {
        createReferenceGenomeProjectSeqType(null)
        sampleType = DomainFactory.createSampleType()

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        assert newDomain.validate()
    }

    @Test
    void testUnique_NoDuplication_WithDifferentSampleType() {
        sampleType = DomainFactory.createSampleType()
        createReferenceGenomeProjectSeqType(null)
        sampleType = DomainFactory.createSampleType()

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        assert newDomain.validate()
    }

    @Test
    void testUnique_NoDuplication_WithDeprecatedDateAndWithoutSampleType() {
        createReferenceGenomeProjectSeqType(new Date())

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        assert newDomain.validate()
    }

    @Test
    void testUnique_NoDuplication_WithDeprecatedDateAndSampleType() {
        sampleType = DomainFactory.createSampleType()
        createReferenceGenomeProjectSeqType(new Date())

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        assert newDomain.validate()
    }

    @Test
    void testUnique_HasDuplication_WithoutSampleType() {
        createReferenceGenomeProjectSeqType(null)

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        TestCase.assertValidateError(newDomain, "referenceGenome", "validator.invalid", referenceGenome)
    }

    @Test
    void testUnique_HasDuplication_WithSampleType() {
        sampleType = DomainFactory.createSampleType()
        createReferenceGenomeProjectSeqType(null)

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        TestCase.assertValidateError(newDomain, "referenceGenome", "validator.invalid", referenceGenome)
    }

    @Test
    void testProjectIsNull() {
        project = null

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        TestCase.assertValidateError(newDomain, "project", "nullable", project)
    }

    @Test
    void testSeqTypeIsNull() {
        seqType = null

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        TestCase.assertValidateError(newDomain, "seqType", "nullable", seqType)
    }

    @Test
    void testReferenceGenomeIsNull() {
        referenceGenome = null

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        TestCase.assertValidateError(newDomain, "referenceGenome", "nullable", referenceGenome)
    }
}
