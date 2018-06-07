package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        Project,
])
class ProjectServiceSpec extends Specification {

    void "createProject: dirName shouldn't overlap with root path"() {
        given:
        ProjectService projectService = new ProjectService([
                configService : new TestConfigService([
                        'otp.root.path'           : "/some/nested/root/path",

                ])
        ])

        when:
        projectService.createProject(
                'project',
                dirName,
                new Realm(),
                AlignmentDeciderBeanNames.NO_ALIGNMENT.bean,
                ['category']
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
}
