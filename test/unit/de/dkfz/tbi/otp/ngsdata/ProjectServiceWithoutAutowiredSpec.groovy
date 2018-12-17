package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.ConfigPerProjectAndSeqType
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.utils.CollectionUtils

@Mock([
        ConfigPerProjectAndSeqType,
        Pipeline,
        Project,
        Realm,
        RunYapsaConfig,
        SeqType,
])
class ProjectServiceWithoutAutowiredSpec extends Specification {

    void "createProject: dirName shouldn't overlap with root path"() {
        given:
        ProjectService projectService = new ProjectService([
                configService: new TestConfigService([
                        (OtpProperty.PATH_PROJECT_ROOT): "/some/nested/root/path",
                ])
        ])

        when:
        projectService.createProject(
                'project',
                dirName,
                new Realm(),
                ['category'],
                QcThresholdHandling.CHECK_AND_NOTIFY
        )

        then:
        AssertionError err = thrown()
        err.message.contains("contains (partial) data processing root path")

        where:
        dirName                         | _
        'some/nested/root/path/dirName' | _
        'nested/root/path/dirName'      | _
        'root/path/dirName'             | _
        'path/dirName'                  | _
        'some/dirName'                  | _
        'nested/dirName'                | _
        'root/dirName'                  | _
    }

    void "test invalidateProjectConfig"() {
        given:
        ProjectService projectService = new ProjectService()
        RunYapsaConfig config = DomainFactory.createRunYapsaConfig()

        when:
        projectService.invalidateProjectConfig(config.project, config.seqType, config.pipeline)

        then:
        config.refresh()
        config.obsoleteDate != null
    }

    void "test createOrUpdateRunYapsaConfig"() {
        given:
        ProjectService projectService = new ProjectService()
        RunYapsaConfig config = DomainFactory.createRunYapsaConfig()

        when:
        projectService.createOrUpdateRunYapsaConfig(config.project, config.seqType, "yapsa 1.0")

        then:
        config.obsoleteDate != null
        RunYapsaConfig newConfig = CollectionUtils.exactlyOneElement(RunYapsaConfig.findAllByProjectAndSeqTypeAndObsoleteDateIsNull(config.project, config.seqType))
        newConfig != config
        newConfig.programVersion == "yapsa 1.0"
    }
}
