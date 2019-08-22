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

    MergingCriteria mergingCriteria_useProject
    MergingCriteria mergingCriteria_useDefault

    SeqPlatformGroup spg_project1
    SeqPlatformGroup spg_project2
    SeqPlatformGroup spg_useDefault1
    SeqPlatformGroup spg_useDefault2

    void setupData() {
        sp1 = new SeqPlatform(name: "sp1")
        sp1.save(flush: true)

        sp2 = new SeqPlatform(name: "sp2")
        sp2.save(flush: true)

        sp3 = new SeqPlatform(name: "sp3")
        sp3.save(flush: true)

        sp4 = new SeqPlatform(name: "sp4")
        sp4.save(flush: true)


        mergingCriteria_useProject = DomainFactory.createMergingCriteriaLazy(
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
        )

        mergingCriteria_useDefault = DomainFactory.createMergingCriteriaLazy(
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT,
        )

        spg_project1 = new SeqPlatformGroup()
        spg_project1.mergingCriteria = mergingCriteria_useProject
        spg_project1.save(flush: true)

        spg_project2 = new SeqPlatformGroup()
        spg_project2.mergingCriteria = mergingCriteria_useProject
        spg_project2.save(flush: true)

        spg_useDefault1 = DomainFactory.createSeqPlatformGroup()

        spg_useDefault2 = DomainFactory.createSeqPlatformGroup()

        sp1.addToSeqPlatformGroups(spg_project1)
        sp1.addToSeqPlatformGroups(spg_useDefault1)
        sp1.save(flush: true)

        sp2.addToSeqPlatformGroups(spg_project1)
        sp2.addToSeqPlatformGroups(spg_useDefault1)
        sp2.save(flush: true)

        sp3.addToSeqPlatformGroups(spg_useDefault1)
        sp3.save(flush: true)

        sp4.addToSeqPlatformGroups(spg_useDefault1)
        sp4.save(flush: true)
    }


    void "test add a SP which belongs already to a default SPG to another default SPG, fails"() {
        given:
        setupData()

        when:
        sp3.addToSeqPlatformGroups(spg_useDefault2)
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
        sp4.addToSeqPlatformGroups(spg_project1)
        sp4.save(flush: true)

        then:
        notThrown(Throwable)
    }

    void "test add a SP which belongs to a default SPG to a project and seqType specific SPG, succeeds"() {
        given:
        setupData()

        when:
        sp4.addToSeqPlatformGroups(spg_project2)
        sp4.save(flush: true)

        then:
        notThrown(Throwable)
    }

    void "test add a SPG which belongs to a specific project and seqType to a SP with a default SPG, succeeds"() {
        given:
        setupData()

        when:
        spg_project2.addToSeqPlatforms(sp4)
        spg_project2.save(flush: true)

        then:
        notThrown(Throwable)
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
        notThrown(Throwable)
    }

    void "test add a SPG which belongs to a specific project and seqType to a SP which already has another SPG with the same project and seqType, fails"() {
        given:
        setupData()

        when:
        spg_project2.addToSeqPlatforms(sp2)
        spg_project2.save(flush: true)

        then:
        thrown(ValidationException)
    }

    void "test add a new SPG which belongs to a specific project and seqType to a SP which already has another SPG with the same project and seqType, fails"() {
        given:
        setupData()

        when:
        SeqPlatformGroup seqPlatformGroup = new SeqPlatformGroup(
                mergingCriteria: mergingCriteria_useProject,
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
        spg_project1 == sp1.getSeqPlatformGroupForMergingCriteria(mergingCriteria_useProject.project, mergingCriteria_useProject.seqType)
        spg_project1 == sp2.getSeqPlatformGroupForMergingCriteria(mergingCriteria_useProject.project, mergingCriteria_useProject.seqType)
        spg_useDefault1 == sp1.getSeqPlatformGroupForMergingCriteria(mergingCriteria_useDefault.project, mergingCriteria_useDefault.seqType)
        spg_useDefault1 == sp2.getSeqPlatformGroupForMergingCriteria(mergingCriteria_useDefault.project, mergingCriteria_useDefault.seqType)
        null == sp3.getSeqPlatformGroupForMergingCriteria(mergingCriteria_useProject.project, mergingCriteria_useProject.seqType)
        null == sp4.getSeqPlatformGroupForMergingCriteria(mergingCriteria_useProject.project, mergingCriteria_useProject.seqType)
    }
}
