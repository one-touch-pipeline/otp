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

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.databinding.BindingFormat

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.DataTableCommand

class ProjectProgressDataTableCommand extends DataTableCommand {

    @BindingFormat('yyyy-MM-dd')
    Date startDate = new Date()
    @BindingFormat('yyyy-MM-dd')
    Date endDate = new Date()

    String projects

    List<String> getProjectNames() {
        if (projects) {
            return projects.split(",") as List
        } else {
            null
        }
    }
}

@Secured("hasRole('ROLE_OPERATOR')")
class ProjectProgressController {

    ProjectService projectService
    ProjectProgressService projectProgressService

    def progress() {
        return [
            startDate: new Date().minus(8).format('yyyy-MM-dd'),
            endDate: new Date().format('yyyy-MM-dd'),
        ]
    }

    JSON dataTableSource(ProjectProgressDataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        List<Project> projects = projectProgressService.getProjectsFromNameList(cmd.projectNames ?: projectService.allProjects*.name)

        //the end date is increased by one day, since the check consider also the time
        List<Run> runs = projectProgressService.getListOfRuns(projects, cmd.startDate, cmd.endDate.plus(1))
        List data = fillTable(runs)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    private List fillTable(List<Run> runs) {
        return runs.collect { Run run ->
            List line = []
            Set<Sample> samples = projectProgressService.getSamples(run)
            line << run.id
            line << run.name
            line << run.seqCenter.toString().toLowerCase()
            line << samples.sort { it.project.name + " "  + it.displayName }.collect { [it.individual.id, it.displayName] }
            return line
        }
    }
}
