package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import grails.test.spock.*
import grails.validation.*


class SeqPlatformIntegrationSpec extends IntegrationSpec {
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

    void setup() {
        sp1 = new SeqPlatform(name: "sp1")
        sp1.save(flush: true, failOnError: true)

        sp2 = new SeqPlatform(name: "sp2")
        sp2.save(flush: true, failOnError: true)

        sp3 = new SeqPlatform(name: "sp3")
        sp3.save(flush: true, failOnError: true)

        sp4 = new SeqPlatform(name: "sp4")
        sp4.save(flush: true, failOnError: true)


        mergingCriteria_useProject = DomainFactory.createMergingCriteriaLazy(
                seqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
        )

        mergingCriteria_useDefault = DomainFactory.createMergingCriteriaLazy(
                seqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT,
        )

        spg_project1 = new SeqPlatformGroup()
        spg_project1.mergingCriteria = mergingCriteria_useProject
        spg_project1.save(flush: true, failOnError: true)

        spg_project2 = new SeqPlatformGroup()
        spg_project2.mergingCriteria = mergingCriteria_useProject
        spg_project2.save(flush: true, failOnError: true)

        spg_useDefault1 = DomainFactory.createSeqPlatformGroup()

        spg_useDefault2 = DomainFactory.createSeqPlatformGroup()

        sp1.addToSeqPlatformGroups(spg_project1)
        sp1.addToSeqPlatformGroups(spg_useDefault1)
        sp1.save(flush: true, failOnError: true)

        sp2.addToSeqPlatformGroups(spg_project1)
        sp2.addToSeqPlatformGroups(spg_useDefault1)
        sp2.save(flush: true, failOnError: true)

        sp3.addToSeqPlatformGroups(spg_useDefault1)
        sp3.save(flush: true, failOnError: true)

        sp4.addToSeqPlatformGroups(spg_useDefault1)
        sp4.save(flush: true, failOnError: true)
    }


    void "test add a SP which belongs to a default SPG to another default SPG, fails"() {
        when:
        sp3.addToSeqPlatformGroups(spg_useDefault2)
        sp3.save(flush: true, failOnError: true)

        then:
        thrown(ValidationException)
    }

    void "test add a SP which belongs to a default SPG to a PST specific SPG with multiple SPs, succeeds"() {
        when:
        sp4.addToSeqPlatformGroups(spg_project1)
        sp4.save(flush: true, failOnError: true)

        then:
        notThrown(Throwable)
    }

    void "test add a SP which belongs to a default SPG to a PST specific SPG, succeeds"() {
        when:
        sp4.addToSeqPlatformGroups(spg_project2)
        sp4.save(flush: true, failOnError: true)

        then:
        notThrown(Throwable)
    }

    void "test add a SPG which belongs to a specific PST to a SP with a default SPG, succeeds"() {
        when:
        spg_project2.addToSeqPlatforms(sp4)
        spg_project2.save(flush: true, failOnError: true)

        then:
        notThrown(Throwable)
    }

    void "test add a SPG which belongs to a specific PST to a SP which already has another SPG with that specific PSt"() {
        when:
        spg_project2.addToSeqPlatforms(sp2)
        spg_project2.save(flush: true, failOnError: true)

        then:
        thrown(ValidationException)
    }


    void "test get correct SPGs for SP and PST"() {
        expect:
        spg_project1 == sp1.getSeqPlatformGroup(mergingCriteria_useProject.project, mergingCriteria_useProject.seqType)
        spg_project1 == sp2.getSeqPlatformGroup(mergingCriteria_useProject.project, mergingCriteria_useProject.seqType)
        spg_useDefault1 == sp1.getSeqPlatformGroup(mergingCriteria_useDefault.project, mergingCriteria_useDefault.seqType)
        spg_useDefault1 == sp2.getSeqPlatformGroup(mergingCriteria_useDefault.project, mergingCriteria_useDefault.seqType)
        null == sp3.getSeqPlatformGroup(mergingCriteria_useProject.project, mergingCriteria_useProject.seqType)
        null == sp4.getSeqPlatformGroup(mergingCriteria_useProject.project, mergingCriteria_useProject.seqType)
    }
}
