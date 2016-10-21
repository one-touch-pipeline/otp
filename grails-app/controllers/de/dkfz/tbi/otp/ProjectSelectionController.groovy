package de.dkfz.tbi.otp

import de.dkfz.tbi.otp.ngsdata.*


class ProjectSelectionController {
    ProjectSelectionService projectSelectionService

    def select(ProjectSelectionCommand cmd) {
        ProjectSelection p = getProjectSelection(cmd.type, cmd.id)
        projectSelectionService.setSelectedProject(p.projects, p.displayName)
        redirect uri: cmd.redirect
    }

    private ProjectSelection getProjectSelection(ProjectSelectionCommand.Type type, Long id) {
        List<Project> projects
        String displayName
        switch (type) {
            case ProjectSelectionCommand.Type.PROJECT:
                projects = [Project.get(id)]
                displayName = Project.get(id).name
                break
            case ProjectSelectionCommand.Type.GROUP:
                projects = Project.findAllByProjectGroup(ProjectGroup.get(id))
                displayName = ProjectGroup.get(id).name
                break
            case ProjectSelectionCommand.Type.CATEGORY:
                projects = ProjectCategory.get(id).projects as List
                displayName = ProjectCategory.get(id).name
                break
            case ProjectSelectionCommand.Type.ALL:
                projects = Project.all
                displayName = g.message(code: "header.projectSelection.allProjects")
                break
            default:
                throw new RuntimeException()
        }
        return new ProjectSelection(projects: projects, displayName: displayName)
    }
}

class ProjectSelectionCommand {
    String displayName
    Type type
    enum Type {
        PROJECT,
        GROUP,
        CATEGORY,
        ALL,
    }
    Long id
    String redirect

    static constraints = {
        redirect(nullable: false, validator: { String val ->
            val.startsWith("/")
        })
    }
}
