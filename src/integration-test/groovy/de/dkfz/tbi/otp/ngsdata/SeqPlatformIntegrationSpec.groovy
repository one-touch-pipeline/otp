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
import grails.validation.ValidationException
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.MergingCriteria

@Rollback
@Integration
class SeqPlatformIntegrationSpec extends Specification {
    SeqPlatform sp1
    SeqPlatform sp2
    SeqPlatform sp3
    SeqPlatform sp4

    MergingCriteria mergingCriteriaUseProject
    MergingCriteria mergingCriteriaUseDefault

    SeqPlatformGroup spgProject1
    SeqPlatformGroup spgProject2
    SeqPlatformGroup spgUseDefault1
    SeqPlatformGroup spgUseDefault2

    void setupData() {
        sp1 = new SeqPlatform(name: "sp1")
        sp1.save(flush: true)

        sp2 = new SeqPlatform(name: "sp2")
        sp2.save(flush: true)

        sp3 = new SeqPlatform(name: "sp3")
        sp3.save(flush: true)

        sp4 = new SeqPlatform(name: "sp4")
        sp4.save(flush: true)


        mergingCriteriaUseProject = DomainFactory.createMergingCriteriaLazy(
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
        )

        mergingCriteriaUseDefault = DomainFactory.createMergingCriteriaLazy(
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT,
        )

        spgProject1 = new SeqPlatformGroup()
        spgProject1.mergingCriteria = mergingCriteriaUseProject
        spgProject1.save(flush: true)

        spgProject2 = new SeqPlatformGroup()
        spgProject2.mergingCriteria = mergingCriteriaUseProject
        spgProject2.save(flush: true)

        spgUseDefault1 = DomainFactory.createSeqPlatformGroup()

        spgUseDefault2 = DomainFactory.createSeqPlatformGroup()

        sp1.addToSeqPlatformGroups(spgProject1)
        sp1.addToSeqPlatformGroups(spgUseDefault1)
        sp1.save(flush: true)

        sp2.addToSeqPlatformGroups(spgProject1)
        sp2.addToSeqPlatformGroups(spgUseDefault1)
        sp2.save(flush: true)

        sp3.addToSeqPlatformGroups(spgUseDefault1)
        sp3.save(flush: true)

        sp4.addToSeqPlatformGroups(spgUseDefault1)
        sp4.save(flush: true)
    }


    void "test add a SP which belongs already to a default SPG to another default SPG, fails"() {
        given:
        setupData()

        when:
        sp3.addToSeqPlatformGroups(spgUseDefault2)
        sp3.save(flush: true)

        then:
        thrown(ValidationException)
    }

    void "test add a SP which belongs already to a default SPG to a new default SPG, fails"() {
        given:
        setupData()

        when:
        SeqPlatformGroup seqPlatformGroup = new SeqPlatformGroup()
        seqPlatformGroup.addToSeqPlatforms(sp3)
        seqPlatformGroup.save(flush: true)

        then:
        thrown(ValidationException)
    }

    void "test add a SP which belongs to a default SPG to a project and seqType specific SPG with multiple SPs, succeeds"() {
        given:
        setupData()

        when:
        sp4.addToSeqPlatformGroups(spgProject1)
        sp4.save(flush: true)

        then:
        noExceptionThrown()
    }

    void "test add a SP which belongs to a default SPG to a project and seqType specific SPG, succeeds"() {
        given:
        setupData()

        when:
        sp4.addToSeqPlatformGroups(spgProject2)
        sp4.save(flush: true)

        then:
        noExceptionThrown()
    }

    void "test add a SPG which belongs to a specific project and seqType to a SP with a default SPG, succeeds"() {
        given:
        setupData()

        when:
        spgProject2.addToSeqPlatforms(sp4)
        spgProject2.save(flush: true)

        then:
        noExceptionThrown()
    }

    void "test add a SPG which belongs to a specific project and seqType to a SP which belongs to another SPG, succeeds"() {
        given:
        setupData()

        when:
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteria([useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC])
        SeqPlatformGroup spg_project3 = DomainFactory.createSeqPlatformGroup(
                mergingCriteria: mergingCriteria
        )
        spg_project3.addToSeqPlatforms(sp2)
        spg_project3.save(flush: true)

        then:
        noExceptionThrown()
    }

    void "test add a SPG which belongs to a specific project and seqType to a SP which already has another SPG with the same project and seqType, fails"() {
        given:
        setupData()

        when:
        spgProject2.addToSeqPlatforms(sp2)
        spgProject2.save(flush: true)

        then:
        thrown(ValidationException)
    }

    void "test add a new SPG which belongs to a specific project and seqType to a SP which already has another SPG with the same project and seqType, fails"() {
        given:
        setupData()

        when:
        SeqPlatformGroup seqPlatformGroup = new SeqPlatformGroup(
                mergingCriteria: mergingCriteriaUseProject,
        )
        seqPlatformGroup.addToSeqPlatforms(sp2)
        seqPlatformGroup.save(flush: true)

        then:
        thrown(ValidationException)
    }


    void "test get correct SPGs for SP and project and seqType"() {
        given:
        setupData()

        expect:
        spgProject1 == sp1.getSeqPlatformGroupForMergingCriteria(mergingCriteriaUseProject.project, mergingCriteriaUseProject.seqType)
        spgProject1 == sp2.getSeqPlatformGroupForMergingCriteria(mergingCriteriaUseProject.project, mergingCriteriaUseProject.seqType)
        spgUseDefault1 == sp1.getSeqPlatformGroupForMergingCriteria(mergingCriteriaUseDefault.project, mergingCriteriaUseDefault.seqType)
        spgUseDefault1 == sp2.getSeqPlatformGroupForMergingCriteria(mergingCriteriaUseDefault.project, mergingCriteriaUseDefault.seqType)
        null == sp3.getSeqPlatformGroupForMergingCriteria(mergingCriteriaUseProject.project, mergingCriteriaUseProject.seqType)
        null == sp4.getSeqPlatformGroupForMergingCriteria(mergingCriteriaUseProject.project, mergingCriteriaUseProject.seqType)
    }
}
