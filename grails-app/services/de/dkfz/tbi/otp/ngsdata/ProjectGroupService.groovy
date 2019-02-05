package de.dkfz.tbi.otp.ngsdata

class ProjectGroupService {

    ProjectService projectService

    List<ProjectGroup> availableProjectGroups() {
        List<Project> projects = projectService.getAllProjects()
        return projects*.projectGroup.unique().findAll { it != null }.sort { it.name }
    }

    ProjectGroup projectGroupByName(String projectGroupName) {
        return ProjectGroup.findByName(projectGroupName)
    }
}
