package de.dkfz.tbi.otp.sampleSwap

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.sampleswap.*
import de.dkfz.tbi.otp.testing.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.SpringSecurityUtils
import spock.lang.Specification

class SampleSwapServiceSpec extends Specification implements UserAndRoles {

    SampleSwapService sampleSwapService
    SeqTrack seqTrack1
    SeqTrack seqTrack11
    SeqTrack seqTrack12
    SeqTrack seqTrack2
    SeqTrack seqTrack3

    def setup() {
        createUserAndRoles()
        Project project1 = DomainFactory.createProject()
        Project project2 = DomainFactory.createProject()
        Project project3 = DomainFactory.createProject()

        Individual individual1 = DomainFactory.createIndividual(project: project1)
        Individual individual2 = DomainFactory.createIndividual(project: project2)
        Individual individual3 = DomainFactory.createIndividual(project: project3)

        Sample sample1 = DomainFactory.createSample(individual: individual1)
        Sample sample2 = DomainFactory.createSample(individual: individual2)
        Sample sample3 = DomainFactory.createSample(individual: individual3)

        seqTrack1 = DomainFactory.createSeqTrack(sample: sample1)
        seqTrack11 = DomainFactory.createSeqTrack(sample: sample1)
        seqTrack12 = DomainFactory.createSeqTrack(sample: sample1)
        seqTrack2 = DomainFactory.createSeqTrack(sample: sample2)
        seqTrack3 = DomainFactory.createSeqTrack(sample: sample3)
    }

    void "test validateInput comment missing, fails"() {
        given:
        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, 1)
        SampleSwapData sampleSwapData11 = createSampleSwapData(seqTrack11, 2)
        SampleSwapData sampleSwapData12 = createSampleSwapData(seqTrack12, 3)
        Map data = [
                data      : [sampleSwapData1, sampleSwapData11, sampleSwapData12],
                comment   : "",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData1.sampleSwapInfos.size() == 0
        sampleSwapData11.sampleSwapInfos.size() == 0
        sampleSwapData12.sampleSwapInfos.size() == 0

        SampleSwapData sampleSwapData = CollectionUtils.exactlyOneElement(data.data.findAll {
            ![sampleSwapData1, sampleSwapData11, sampleSwapData12].contains(it)
        })
        sampleSwapData.sampleSwapInfos.size() == 1

        SampleSwapInfo sampleSwapInfo = CollectionUtils.exactlyOneElement(sampleSwapData.sampleSwapInfos.findAll {
            it.level == SampleSwapLevel.ERROR
        })

        sampleSwapInfo.description == "Comment is not allowed to be empty."
    }

    void "test validateInput with different project for one entry, fails"() {
        given:
        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, [project: seqTrack2.project.name], 1)
        SampleSwapData sampleSwapData11 = createSampleSwapData(seqTrack11, 2)
        SampleSwapData sampleSwapData12 = createSampleSwapData(seqTrack12, 3)
        Map data = [
                data      : [sampleSwapData1, sampleSwapData11, sampleSwapData12],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData1.sampleSwapInfos.size() == 3
        SampleSwapInfo sampleSwapInfo = CollectionUtils.exactlyOneElement(sampleSwapData1.sampleSwapInfos.findAll {
            it.level == SampleSwapLevel.ERROR
        })
        sampleSwapInfo.description == "Column 'project' of '1' can't change from '${seqTrack1.project.name}' to '${seqTrack2.project.name}' because pid '${seqTrack1.individual.pid}' already exists in project '${seqTrack1.project.name}'."
        sampleSwapData11.sampleSwapInfos.size() == 0
        sampleSwapData12.sampleSwapInfos.size() == 0
    }

    void "test validateInput with different project for all entries, succeeds"() {
        given:
        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, [project: seqTrack2.project.name], 1)
        SampleSwapData sampleSwapData11 = createSampleSwapData(seqTrack11, [project: seqTrack2.project.name], 2)
        SampleSwapData sampleSwapData12 = createSampleSwapData(seqTrack12, [project: seqTrack2.project.name], 3)
        Map data = [
                data      : [sampleSwapData1, sampleSwapData11, sampleSwapData12],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData1.sampleSwapInfos.size() == 2
        sampleSwapData1.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        sampleSwapData11.sampleSwapInfos.size() == 2
        sampleSwapData11.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        sampleSwapData12.sampleSwapInfos.size() == 2
        sampleSwapData12.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        SampleSwapData sampleSwapData = CollectionUtils.exactlyOneElement(data.data.findAll {
            ![sampleSwapData1, sampleSwapData11, sampleSwapData12].contains(it)
        })
        sampleSwapData.sampleSwapInfos.size() == 1
        sampleSwapData.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0
    }

    void "test validateInput with empty pid for one entry, fails"() {
        given:
        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, [pid: ""], 1)
        SampleSwapData sampleSwapData11 = createSampleSwapData(seqTrack11, 2)
        SampleSwapData sampleSwapData12 = createSampleSwapData(seqTrack12, 3)
        Map data = [
                data      : [sampleSwapData1, sampleSwapData11, sampleSwapData12],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData1.sampleSwapInfos.size() == 2
        SampleSwapInfo sampleSwapInfo1 = CollectionUtils.exactlyOneElement(sampleSwapData1.sampleSwapInfos.findAll {
            it.level == SampleSwapLevel.ERROR
        })
        sampleSwapInfo1.description == "Column 'pid' of '1' can't be empty."

        sampleSwapData11.sampleSwapInfos.size() == 0
        sampleSwapData12.sampleSwapInfos.size() == 0
    }

    void "test validateInput with different already existing pid and non matching project for one entry, fails"() {
        given:
        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, [pid: seqTrack2.individual.pid], 1)
        SampleSwapData sampleSwapData11 = createSampleSwapData(seqTrack11, 2)
        SampleSwapData sampleSwapData12 = createSampleSwapData(seqTrack12, 3)
        Map data = [
                data      : [sampleSwapData1, sampleSwapData11, sampleSwapData12],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData1.sampleSwapInfos.size() == 2
        SampleSwapInfo sampleSwapInfo1 = CollectionUtils.exactlyOneElement(sampleSwapData1.sampleSwapInfos.findAll {
            it.level == SampleSwapLevel.ERROR
        })
        sampleSwapInfo1.description == "Column 'pid' of '1' can't change from '${seqTrack1.individual.pid}' to '${seqTrack2.individual.pid}' because '${seqTrack2.individual.pid}' already exists in 'project' '${seqTrack2.project.name}'."

        sampleSwapData11.sampleSwapInfos.size() == 0
        sampleSwapData12.sampleSwapInfos.size() == 0
    }

    void "test validateInput with different already existing pid and non matching project for all entries, fails"() {
        given:
        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, [pid: seqTrack2.individual.pid], 1)
        SampleSwapData sampleSwapData11 = createSampleSwapData(seqTrack11, [pid: seqTrack2.individual.pid], 2)
        SampleSwapData sampleSwapData12 = createSampleSwapData(seqTrack12, [pid: seqTrack2.individual.pid], 3)
        Map data = [
                data      : [sampleSwapData1, sampleSwapData11, sampleSwapData12],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData1.sampleSwapInfos.size() == 2

        SampleSwapInfo sampleSwapInfo1 = CollectionUtils.exactlyOneElement(sampleSwapData1.sampleSwapInfos.findAll {
            it.level == SampleSwapLevel.ERROR
        })
        sampleSwapInfo1.description == "Column 'pid' of '1' can't change from '${seqTrack1.individual.pid}' to '${seqTrack2.individual.pid}' because '${seqTrack2.individual.pid}' already exists in 'project' '${seqTrack2.individual.project.name}'."



        sampleSwapData11.sampleSwapInfos.size() == 2
        SampleSwapInfo sampleSwapInfo11 = CollectionUtils.exactlyOneElement(sampleSwapData11.sampleSwapInfos.findAll {
            it.level == SampleSwapLevel.ERROR
        })
        sampleSwapInfo11.description == "Column 'pid' of '2' can't change from '${seqTrack1.individual.pid}' to '${seqTrack2.individual.pid}' because '${seqTrack2.individual.pid}' already exists in 'project' '${seqTrack2.individual.project.name}'."

        sampleSwapData12.sampleSwapInfos.size() == 2
        SampleSwapInfo sampleSwapInfo12 = CollectionUtils.exactlyOneElement(sampleSwapData12.sampleSwapInfos.findAll {
            it.level == SampleSwapLevel.ERROR
        })
        sampleSwapInfo12.description == "Column 'pid' of '3' can't change from '${seqTrack1.individual.pid}' to '${seqTrack2.individual.pid}' because '${seqTrack2.individual.pid}' already exists in 'project' '${seqTrack2.individual.project.name}'."
    }

    void "test validateInput with difateInput with new pids in different project for all entries, succeeds"() {
        given:
        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, [pid: seqTrack2.individual.pid, project: seqTrack2.individual.project.name], 1)
        SampleSwapData sampleSwapData11 = createSampleSwapData(seqTrack11, [pid: seqTrack2.individual.pid, project: seqTrack2.individual.project.name], 2)
        SampleSwapData sampleSwapData12 = createSampleSwapData(seqTrack12, [pid: seqTrack2.individual.pid, project: seqTrack2.individual.project.name], 3)
        Map data = [
                data      : [sampleSwapData1, sampleSwapData11, sampleSwapData12],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData1.sampleSwapInfos.size() == 2
        sampleSwapData1.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        sampleSwapData11.sampleSwapInfos.size() == 2
        sampleSwapData11.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        sampleSwapData12.sampleSwapInfos.size() == 2
        sampleSwapData12.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        SampleSwapData sampleSwapData = CollectionUtils.exactlyOneElement(data.data.findAll {
            ![sampleSwapData1, sampleSwapData11, sampleSwapData12].contains(it)
        })
        sampleSwapData.sampleSwapInfos.size() == 1
        sampleSwapData.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0
    }

    void "test validateInput with different new pid for one entry, succeeds"() {
        given:
        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, [pid: "newTestPid"], 1)
        SampleSwapData sampleSwapData11 = createSampleSwapData(seqTrack1, 2)
        SampleSwapData sampleSwapData12 = createSampleSwapData(seqTrack12, 3)
        Map data = [
                data      : [sampleSwapData1, sampleSwapData11, sampleSwapData12],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData1.sampleSwapInfos.size() == 1
        sampleSwapData1.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        SampleSwapData sampleSwapData = CollectionUtils.exactlyOneElement(data.data.findAll {
            ![sampleSwapData1, sampleSwapData11, sampleSwapData12].contains(it)
        })
        sampleSwapData.sampleSwapInfos.size() == 1
        sampleSwapData.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0
    }

    void "test validateInput with new pid for all entries, succeeds"() {
        given:
        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, [pid: "newTestPid"], 1)
        SampleSwapData sampleSwapData11 = createSampleSwapData(seqTrack11, [pid: "newTestPid"], 2)
        SampleSwapData sampleSwapData12 = createSampleSwapData(seqTrack12, [pid: "newTestPid"], 3)
        Map data = [
                data      : [sampleSwapData1, sampleSwapData11, sampleSwapData12],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData1.sampleSwapInfos.size() == 1
        sampleSwapData1.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        sampleSwapData11.sampleSwapInfos.size() == 1
        sampleSwapData11.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        sampleSwapData12.sampleSwapInfos.size() == 1
        sampleSwapData12.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        SampleSwapData sampleSwapData = CollectionUtils.exactlyOneElement(data.data.findAll {
            ![sampleSwapData1, sampleSwapData11, sampleSwapData12].contains(it)
        })
        sampleSwapData.sampleSwapInfos.size() == 1
        sampleSwapData.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0
    }

    void "test validateInput with new pid in different project for one entry, succeeds"() {
        given:
        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, [project: seqTrack2.project.name, pid: "newTestPid"], 1)
        SampleSwapData sampleSwapData11 = createSampleSwapData(seqTrack11, 2)
        SampleSwapData sampleSwapData12 = createSampleSwapData(seqTrack12, 3)
        Map data = [
                data      : [sampleSwapData1, sampleSwapData11, sampleSwapData12],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData1.sampleSwapInfos.size() == 2
        sampleSwapData1.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        sampleSwapData11.sampleSwapInfos.size() == 0

        sampleSwapData12.sampleSwapInfos.size() == 0

        SampleSwapData sampleSwapData = CollectionUtils.exactlyOneElement(data.data.findAll {
            ![sampleSwapData1, sampleSwapData11, sampleSwapData12].contains(it)
        })
        sampleSwapData.sampleSwapInfos.size() == 1
        sampleSwapData.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0
    }

    void "test validateInput with new pid in different project for all entries, succeeds"() {
        given:
        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, [project: seqTrack2.project.name, pid: "newTestPid"], 1)
        SampleSwapData sampleSwapData11 = createSampleSwapData(seqTrack11, [project: seqTrack2.project.name, pid: "newTestPid"], 2)
        SampleSwapData sampleSwapData12 = createSampleSwapData(seqTrack12, [project: seqTrack2.project.name, pid: "newTestPid"], 3)
        Map data = [
                data      : [sampleSwapData1, sampleSwapData11, sampleSwapData12],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData1.sampleSwapInfos.size() == 2
        sampleSwapData1.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        sampleSwapData11.sampleSwapInfos.size() == 2
        sampleSwapData11.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        sampleSwapData12.sampleSwapInfos.size() == 2
        sampleSwapData12.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        SampleSwapData sampleSwapData = CollectionUtils.exactlyOneElement(data.data.findAll {
            ![sampleSwapData1, sampleSwapData11, sampleSwapData12].contains(it)
        })
        sampleSwapData.sampleSwapInfos.size() == 1
        sampleSwapData.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0
    }

    void "test validateInput with new pids in different project for all entries, succeeds"() {
        given:
        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, [project: seqTrack2.project.name, pid: "newTestPid1"], 1)
        SampleSwapData sampleSwapData11 = createSampleSwapData(seqTrack11, [project: seqTrack2.project.name, pid: "newTestPid2"], 2)
        SampleSwapData sampleSwapData12 = createSampleSwapData(seqTrack12, [project: seqTrack2.project.name, pid: "newTestPid3"], 3)
        Map data = [
                data      : [sampleSwapData1, sampleSwapData11, sampleSwapData12],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData1.sampleSwapInfos.size() == 2
        sampleSwapData1.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        sampleSwapData11.sampleSwapInfos.size() == 2
        sampleSwapData11.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        sampleSwapData12.sampleSwapInfos.size() == 2
        sampleSwapData12.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        SampleSwapData sampleSwapData = CollectionUtils.exactlyOneElement(data.data.findAll {
            ![sampleSwapData1, sampleSwapData11, sampleSwapData12].contains(it)
        })
        sampleSwapData.sampleSwapInfos.size() == 4
        sampleSwapData.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0
    }

    void "test validateInput already existing pid into two different projects, fails"() {
        given:
        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, [pid: seqTrack2.individual.pid, project: seqTrack2.project.name], 1)
        SampleSwapData sampleSwapData11 = createSampleSwapData(seqTrack11, 2)
        SampleSwapData sampleSwapData12 = createSampleSwapData(seqTrack12, [pid: seqTrack2.individual.pid, project: seqTrack3.project.name], 3)
        Map data = [
                data      : [sampleSwapData1, sampleSwapData11, sampleSwapData12],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData1.sampleSwapInfos.size() == 4
        sampleSwapData1.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 2

        sampleSwapData11.sampleSwapInfos.size() == 0

        sampleSwapData12.sampleSwapInfos.size() == 4
        sampleSwapData12.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 2

        SampleSwapData sampleSwapData = CollectionUtils.exactlyOneElement(data.data.findAll {
            ![sampleSwapData1, sampleSwapData11, sampleSwapData12].contains(it)
        })
        sampleSwapData.sampleSwapInfos.size() == 1

        SampleSwapInfo sampleSwapInfo = CollectionUtils.exactlyOneElement(sampleSwapData.sampleSwapInfos.findAll {
            it.level == SampleSwapLevel.ERROR
        })

        sampleSwapInfo.description == "You are trying to move the 'individual' '${seqTrack2.individual.pid}' into multiple projects, this is not allowed. Corresponding rows: '1, 3'."
    }

    void "test validateInput with new pid for one entry and already existing entry for another entry, succeeds"() {
        given:
        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, [pid: "newTestPid"], 1)
        SampleSwapData sampleSwapData11 = createSampleSwapData(seqTrack11, [pid: DomainFactory.createIndividual(project: seqTrack11.project).pid], 2)
        Map data = [
                data      : [sampleSwapData1, sampleSwapData11],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData1.sampleSwapInfos.size() == 1
        sampleSwapData1.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        sampleSwapData11.sampleSwapInfos.size() == 1
        sampleSwapData11.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0

        SampleSwapData sampleSwapData = CollectionUtils.exactlyOneElement(data.data.findAll {
            ![sampleSwapData1, sampleSwapData11].contains(it)
        })
        sampleSwapData.sampleSwapInfos.size() == 2
        sampleSwapData.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0
    }

    void "test validateInput with empty Pid, fails"() {
        given:
        SampleSwapData sampleSwapData = createSampleSwapData(seqTrack1, [pid: ""], 1)
        Map data = [
                data      : [sampleSwapData],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData.sampleSwapInfos.size() == 2
        SampleSwapInfo sampleSwapInfo = CollectionUtils.exactlyOneElement(sampleSwapData.sampleSwapInfos.findAll {
            it.level == SampleSwapLevel.ERROR
        })

        sampleSwapInfo.description == "Column 'pid' of '1' can't be empty."
    }

    void "test validateInput with project changed to deep and invalid pid, shows warnings"() {
        given:
        SampleSwapData sampleSwapData = createSampleSwapData(seqTrack1, [project: "DEEP"], 1)
        Map data = [
                data      : [sampleSwapData],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData.sampleSwapInfos.size() == 3
        SampleSwapInfo sampleSwapInfo = CollectionUtils.exactlyOneElement(sampleSwapData.sampleSwapInfos.findAll {
            it.level == SampleSwapLevel.WARNING
        })

        sampleSwapInfo.description == "Column 'pid' of '1' didn't change from '${seqTrack1.individual.pid}', but 'project' is 'DEEP' and the 'pid' doesn't conform to the naming scheme conventions."
    }

    void "test validateInput with project deep and invalid pid,  shows warnings"() {
        given:
        seqTrack1.individual.project = DomainFactory.createProject(name: "DEEP")
        seqTrack1.individual.pid = "41_Hf01_BlAd_CD_WGBS_S_1"
        SampleSwapData sampleSwapData = createSampleSwapData(seqTrack1, [pid: "invalidPid"], 1)
        Map data = [
                data      : [sampleSwapData],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData.sampleSwapInfos.size() == 2
        SampleSwapInfo sampleSwapInfo = CollectionUtils.exactlyOneElement(sampleSwapData.sampleSwapInfos.findAll {
            it.level == SampleSwapLevel.WARNING
        })

        sampleSwapInfo.description == "Column 'pid' of '1' can't change from '41_Hf01_BlAd_CD_WGBS_S_1' to 'invalidPid' because 'project' is 'DEEP' and the 'pid' doesn't conform to the naming scheme conventions."
    }

    void "test validateInput with project deep and valid pid, succeeds"() {
        given:
        SampleSwapData sampleSwapData = createSampleSwapData(seqTrack1, [project: "DEEP", pid: "41_Hf01_BlAd_CD"], 1)
        Map data = [
                data      : [sampleSwapData],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData.sampleSwapInfos.size() == 2
        sampleSwapData.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0
    }

    void "test validateInput with project hipo and invalid datafile name, fails"() {
        given:
        seqTrack1.individual.project = DomainFactory.createProject(name: "hipo059")
        seqTrack1.individual.pid = "H059-ABCDEF"
        DataFile dataFile1 = DomainFactory.createDataFile(seqTrack: seqTrack1, fileName: "H059-ABCDEF_filename")
        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, [files: ["${dataFile1.id}": "invalidFileName"]], 1)

        seqTrack11.individual.pid = "H059-ABCDEF"
        DomainFactory.createDataFile(seqTrack: seqTrack11, fileName: "H059-ABCDEF_filename")
        SampleSwapData sampleSwapData2 = createSampleSwapData(seqTrack11, [pid: "H059-ABCDEG", project: "hipo059"], 2)


        Map data = [
                data      : [sampleSwapData1, sampleSwapData2],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData1.sampleSwapInfos.size() == 2
        SampleSwapInfo sampleSwapInfo1 = CollectionUtils.exactlyOneElement(sampleSwapData1.sampleSwapInfos.findAll {
            it.level == SampleSwapLevel.ERROR
        })
        sampleSwapInfo1.description == "Column 'datafiles 1' of '1' with value 'invalidFileName' has to start with 'H059-ABCDEF'."

        sampleSwapData2.sampleSwapInfos.size() == 3

        Set sampleSwapInfos = sampleSwapData2.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }
        sampleSwapInfos.size() == 2
        sampleSwapInfos*.description ==
                [
                        "Column 'datafiles 1' of '2' with value 'H059-ABCDEF_filename' has to start with 'H059-ABCDEG'.",
                        ""
                ]
    }

    void "test validateInput with project hipo and valid datafile name, succeeds"() {
        given:
        seqTrack1.individual.project = DomainFactory.createProject(name: "hipo059")
        seqTrack1.individual.pid = "H059-ABCDEF"
        DataFile dataFile = DomainFactory.createDataFile(seqTrack: seqTrack1, fileName: "H059-ABCDEF_filename")
        SampleSwapData sampleSwapData = createSampleSwapData(seqTrack1, [pid: "H059-ABCDEG", files: ["${dataFile.id}": "H059-ABCDEG_filename"]], 1)
        Map data = [
                data      : [sampleSwapData],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData.sampleSwapInfos.size() == 2
        sampleSwapData.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }.size() == 0
    }

    void "test validateInput antibody and antibodyTarget without seqType ChIP, fails"() {
        given:
        seqTrack1.seqType = DomainFactory.createSeqType(name: "ChIP")
        AntibodyTarget antibodyTarget = DomainFactory.createAntibodyTarget()
        DomainFactory.createSeqType(displayName: "newSeqType")
        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, [seqType: "newSeqType", antibodyTarget: antibodyTarget.name, antibody: "antibody"], 1)

        SampleSwapData sampleSwapData2 = createSampleSwapData(seqTrack11, [antibodyTarget: antibodyTarget.name, antibody: "antibody"], 2)


        Map data = [
                data      : [sampleSwapData1, sampleSwapData2],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData1.sampleSwapInfos.size() == 5
        Set sampleSwapInfos1 = sampleSwapData1.sampleSwapInfos.findAll { it.level == SampleSwapLevel.WARNING }
        sampleSwapInfos1.size() == 2
        sampleSwapInfos1*.description ==
                [
                        "Column 'antibody' of '1' can't change from '' to 'antibody' because 'seqType' is 'newSeqType' and not 'ChIP'.",
                        "Column 'antibodyTarget' of '1' can't change from '' to '${antibodyTarget.name}' because 'seqType' is 'newSeqType' and not 'ChIP'."

                ]

        sampleSwapData2.sampleSwapInfos.size() == 4
        Set sampleSwapInfos2 = sampleSwapData2.sampleSwapInfos.findAll { it.level == SampleSwapLevel.WARNING }
        sampleSwapInfos2.size() == 2
        sampleSwapInfos2*.description ==
                [
                        "Column 'antibody' of '2' can't change from '' to 'antibody' because 'seqType' is '${seqTrack11.seqType.name}' and not 'ChIP'.",
                        "Column 'antibodyTarget' of '2' can't change from '' to '${antibodyTarget.name}' because 'seqType' is '${seqTrack11.seqType.name}' and not 'ChIP'."

                ]
    }

    void "test validateInput with project ChIP no antibodyTarget, fails"() {
        given:
        SeqType seqType = DomainFactory.createSeqType(name: "ChIP")
        DomainFactory.createAntibodyTarget(name: "antibodyTarget")

        LibraryPreparationKit libraryPreparationKit = DomainFactory.createLibraryPreparationKit()
        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, [seqType: seqType.name, libPrepKit: libraryPreparationKit.name], 1)

        seqTrack11.seqType = seqType
        SampleSwapData sampleSwapData2 = createSampleSwapData(seqTrack11, [libPrepKit: libraryPreparationKit.name], 2)
        sampleSwapData2.oldValues.antibodyTarget = "antibodyTarget"

        Map data = [
                data      : [sampleSwapData1, sampleSwapData2],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData1.sampleSwapInfos.size() == 4
        Set sampleSwapInfos1 = sampleSwapData1.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }
        sampleSwapInfos1.size() == 2
        sampleSwapInfos1*.description == ["", "Column 'antibodyTarget' of '1' must be filled in because 'seqType' was set from '${seqTrack1.seqType.name}' to '${seqType.name}'."]

        sampleSwapData2.sampleSwapInfos.size() == 3
        SampleSwapInfo sampleSwapInfo2 = CollectionUtils.exactlyOneElement(sampleSwapData2.sampleSwapInfos.findAll {
            it.level == SampleSwapLevel.ERROR
        })
        sampleSwapInfo2.description == "Column 'antibodyTarget' of '2' must be filled in because 'seqType' is 'ChIP'."
    }

    void "test validateInput with project ChIP no libPrepKit, fails"() {
        given:
        SeqType seqType = DomainFactory.createSeqType(name: "ChIP")
        AntibodyTarget antibodyTarget = DomainFactory.createAntibodyTarget()
        DomainFactory.createLibraryPreparationKit(name: "libPrepKit")
        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, [seqType: seqType.name, antibodyTarget: antibodyTarget.name], 1)

        seqTrack11.seqType = seqType
        SampleSwapData sampleSwapData2 = createSampleSwapData(seqTrack11, [antibodyTarget: antibodyTarget.name], 2)
        sampleSwapData2.oldValues.libPrepKit = "libPrepKit"


        Map data = [
                data      : [sampleSwapData1, sampleSwapData2],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData1.sampleSwapInfos.size() == 4
        Set sampleSwapInfos1 = sampleSwapData1.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }
        sampleSwapInfos1.size() == 2
        sampleSwapInfos1*.description == ["", "Column 'libPrepKit' of '1' must be filled in because 'seqType' was set from '${seqTrack1.seqType.name}' to '${seqType.name}'."]

        sampleSwapData2.sampleSwapInfos.size() == 3
        SampleSwapInfo sampleSwapInfo2 = CollectionUtils.exactlyOneElement(sampleSwapData2.sampleSwapInfos.findAll {
            it.level == SampleSwapLevel.ERROR
        })
        sampleSwapInfo2.description == "Column 'libPrepKit' of '2' must be filled in because 'seqType' is '${seqType.name}'."
    }
    void "test validateInput with SeqType EXOME, WGBS and RNA no libPrepKit, fails"() {
        given:
        SeqType exome = DomainFactory.createExomeSeqType()
        SeqType wgbs = DomainFactory.createWholeGenomeBisulfiteSeqType()
        SeqType rna = DomainFactory.createRnaPairedSeqType()

        SampleSwapData sampleSwapData1 = createSampleSwapData(seqTrack1, [seqType: exome.displayName], 1)
        SampleSwapData sampleSwapData11 = createSampleSwapData(seqTrack11, [seqType: wgbs.displayName], 2)
        SampleSwapData sampleSwapData12 = createSampleSwapData(seqTrack12, [seqType: rna.displayName], 3)


        Map data = [
                data      : [sampleSwapData1, sampleSwapData11,sampleSwapData12],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        Set sampleSwapInfos1 = sampleSwapData1.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }
        sampleSwapInfos1.size() == 2
        sampleSwapInfos1*.description == ["", "Column 'libPrepKit' of '1' must be filled in because 'seqType' was set from '${seqTrack1.seqType.name}' to 'EXOME'."]

        sampleSwapData11.sampleSwapInfos.size() == 3
        Set sampleSwapInfos11 = sampleSwapData11.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }
        sampleSwapInfos11.size() == 2
        sampleSwapInfos11*.description == ["", "Column 'libPrepKit' of '2' must be filled in because 'seqType' was set from '${seqTrack11.seqType.name}' to 'WGBS'."]

        sampleSwapData12.sampleSwapInfos.size() == 3
        Set sampleSwapInfos12 = sampleSwapData12.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }
        sampleSwapInfos12.size() == 2
        sampleSwapInfos12*.description == ["", "Column 'libPrepKit' of '3' must be filled in because 'seqType' was set from '${seqTrack12.seqType.name}' to 'RNA'."]
    }

    void "test validateInput with not registered sampleType, seqType antiBodyTarget and libraryPreparationKit, fails"() {
        given:
        SampleSwapData sampleSwapData = createSampleSwapData(seqTrack1, [sampleType: "invalid", seqType: "invalid", antibodyTarget: "invalid", libPrepKit: "invalid"], 1)


        Map data = [
                data      : [sampleSwapData],
                comment   : "comment",
                individual: seqTrack1.individual,
        ]

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.validateInput(data)
        }

        then:
        sampleSwapData.sampleSwapInfos.size() == 9
        Set sampleSwapInfos = sampleSwapData.sampleSwapInfos.findAll { it.level == SampleSwapLevel.ERROR }
        sampleSwapInfos.size() == 4
        sampleSwapInfos*.description == [
                "Column 'antibodyTarget' of '1' with value 'invalid' is not registered in the OTP database.",
                "Column 'libPrepKit' of '1' with value 'invalid' is not registered in the OTP database.",
                "Column 'sampleType' of '1' with value 'invalid' is not registered in the OTP database.",
                "Column 'seqType' of '1' with value 'invalid' is not registered in the OTP database.",
        ]
    }


    private SampleSwapData createSampleSwapData(SeqTrack seqTrack, Map changedData = [:], int rowNumber) {
        return new SampleSwapData(sampleSwapService.getPropertiesForSampleSwap(seqTrack), sampleSwapService.getPropertiesForSampleSwap(seqTrack) + changedData, Long.toString(seqTrack.id), rowNumber)
    }
}
