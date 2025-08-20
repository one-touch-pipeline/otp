/*
 * Copyright 2011-2025 The OTP authors
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
import grails.gorm.transactions.Rollback
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.project.Project

@Rollback
@Integration
class ReferenceGenomeProjectSeqTypeIntegrationSpec extends Specification {

    Project project
    SeqType seqType
    SampleType sampleType
    ReferenceGenome referenceGenome

    void setupData() {
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

    void "test unique constraint allows no duplication with different projects"() {
        given:
        setupData()
        createReferenceGenomeProjectSeqType(null)
        project = DomainFactory.createProject()

        when:
        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)

        then:
        newDomain.validate()
    }

    void "test unique constraint allows no duplication with different seqType"() {
        given:
        setupData()
        createReferenceGenomeProjectSeqType(null)
        seqType = DomainFactory.createSeqType()

        when:
        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)

        then:
        newDomain.validate()
    }

    void "test unique constraint allows no duplication with and without sampleType"() {
        given:
        setupData()
        createReferenceGenomeProjectSeqType(null)
        sampleType = DomainFactory.createSampleType()

        when:
        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)

        then:
        newDomain.validate()
    }

    void "test unique constraint allows no duplication with different sampleType"() {
        given:
        setupData()
        sampleType = DomainFactory.createSampleType()
        createReferenceGenomeProjectSeqType(null)
        sampleType = DomainFactory.createSampleType()

        when:
        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)

        then:
        newDomain.validate()
    }

    void "test unique constraint allows no duplication with deprecated date and without sampleType"() {
        given:
        setupData()
        createReferenceGenomeProjectSeqType(new Date())

        when:
        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)

        then:
        newDomain.validate()
    }

    void "test unique constraint allows no duplication with deprecated date and sampleType"() {
        given:
        setupData()
        sampleType = DomainFactory.createSampleType()
        createReferenceGenomeProjectSeqType(new Date())

        when:
        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)

        then:
        newDomain.validate()
    }

    void "test unique constraint detects duplication without sampleType"() {
        given:
        setupData()
        createReferenceGenomeProjectSeqType(null)

        when:
        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)

        then:
        TestCase.assertValidateError(newDomain, "referenceGenome", "validator.invalid", referenceGenome)
    }

    void "test unique constraint detects duplication with sampleType"() {
        given:
        setupData()
        sampleType = DomainFactory.createSampleType()
        createReferenceGenomeProjectSeqType(null)

        when:
        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)

        then:
        TestCase.assertValidateError(newDomain, "referenceGenome", "validator.invalid", referenceGenome)
    }

    void "test validation fails when project is null"() {
        given:
        setupData()
        project = null

        when:
        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)

        then:
        TestCase.assertValidateError(newDomain, "project", "nullable", project)
    }

    void "test validation fails when seqType is null"() {
        given:
        setupData()
        seqType = null

        when:
        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)

        then:
        TestCase.assertValidateError(newDomain, "seqType", "nullable", seqType)
    }

    void "test validation fails when referenceGenome is null"() {
        given:
        setupData()
        referenceGenome = null

        when:
        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)

        then:
        TestCase.assertValidateError(newDomain, "referenceGenome", "nullable", referenceGenome)
    }
}
