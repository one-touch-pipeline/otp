package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.testing.UserAndRoles
import grails.plugin.springsecurity.SpringSecurityUtils
import spock.lang.Specification


class ProjectServiceSpec extends Specification implements UserAndRoles {

    ProjectService projectService

    def setup() {
        createUserAndRoles()
        DomainFactory.createProject(name: 'testProject', nameInMetadataFiles: 'testProject2', dirName: 'testDir')
        DomainFactory.createProject(name: 'testProject3', nameInMetadataFiles: null)
        ProjectGroup projectGroup = new ProjectGroup(name: 'projectGroup')
        projectGroup.save(flush: true, failOnError: true)
    }

    void "test createProject valid input"() {
        when:
        Project project
        SpringSecurityUtils.doWithAuth("admin"){
            project = projectService.createProject(name,  dirName,  'DKFZ_13.1', 'noAlignmentDecider',  projectGroup,  nameInMetadataFiles,  copyFiles)
        }

        then:
        project.name == name
        project.dirName == dirName
        project.projectGroup == ProjectGroup.findByName(projectGroup)
        project.nameInMetadataFiles == nameInMetadataFiles
        project.hasToBeCopied == copyFiles



        where:
        name        | dirName   | projectGroup      | nameInMetadataFiles   | copyFiles
        'project'   | 'dir'     | ''                | 'project'             | true
        'project'   | 'dir'     | ''                | null                  | true
        'project'   | 'dir'     | 'projectGroup'    | 'project'             | true
        'project'   | 'dir'     | ''                | 'project'             | false
    }

    void "test createProject invalid input"() {
        when:
        Project project
        SpringSecurityUtils.doWithAuth("admin"){
            project = projectService.createProject(name, dirName, 'DKFZ_13.1', 'noAlignmentDecider', projectGroup, nameInMetadataFiles, copyFiles)
        }

        then:
            grails.validation.ValidationException ex = thrown()
            ex.message.contains(errorName) && ex.message.contains(errorLocaction)

        where:
        name            | dirName   | projectGroup  | nameInMetadataFiles   | copyFiles || errorName                            | errorLocaction
        'testProject'   | 'dir'     | ''            | 'project'             | true      || 'unique'                                                                                     | 'on field \'name\': rejected value [testProject]'
        'testProject2'  | 'dir'     | ''            | 'project'             | true      || 'this name is already used in another project as nameInMetadataFiles entry'                  | 'on field \'name\': rejected value [testProject2]'
        'project'       | 'dir'     | ''            | 'testProject'         | true      || 'this nameInMetadataFiles is already used in another project as name entry'                  | 'on field \'nameInMetadataFiles\': rejected value [testProject]'
        'project'       | 'dir'     | ''            | 'testProject2'        | true      || 'this nameInMetadataFiles is already used in another project as nameInMetadataFiles entry'   | 'on field \'nameInMetadataFiles\': rejected value [testProject2]'
        'project'       | 'dir'     | ''            | ''                    | true      || 'blank'                                                                                      | 'on field \'nameInMetadataFiles\': rejected value []'
        'project'       | 'testDir' | ''            | ''                    | true      || 'unique'                                                                                     | 'on field \'dirName\': rejected value [testDir]'
    }

    void "test updateNameInMetadata valid input"() {
        when:
        Project project = Project.findByName("testProject")
        SpringSecurityUtils.doWithAuth("admin"){
            projectService.updateNameInMetadata(name, project)
        }

        then:
        project.nameInMetadataFiles == name

        where:
        name                | _
        'testProject'       | _
        'testProject2'      | _
        'newTestProject'    | _
        null                | _
    }

    void "test updateNameInMetadata invalid input"() {
        when:
        Project project = Project.findByName("testProject3")
        SpringSecurityUtils.doWithAuth("admin"){
            projectService.updateNameInMetadata(name, project)
        }

        then:
        grails.validation.ValidationException ex = thrown()
        ex.message.contains(errorName) && ex.message.contains(errorLocaction)


        where:
        name            || errorName                                                                                    | errorLocaction
        'testProject'   || 'this nameInMetadataFiles is already used in another project as name entry'                  | 'on field \'nameInMetadataFiles\': rejected value [testProject]'
        'testProject2'  || 'this nameInMetadataFiles is already used in another project as nameInMetadataFiles entry'   | 'on field \'nameInMetadataFiles\': rejected value [testProject2]'
        ''              || 'blank'                                                                                      | 'on field \'nameInMetadataFiles\': rejected value []'
    }
}
