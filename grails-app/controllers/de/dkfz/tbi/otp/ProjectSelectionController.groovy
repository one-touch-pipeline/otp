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
