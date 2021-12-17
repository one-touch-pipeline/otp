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

import de.dkfz.tbi.otp.project.Project

//---------------------------
//input
String projectName = ''

String newPrefix = ''

/* this must 'true' if you want to set the prefix for more than 1 project or 'null'
 * if true -> it will mark all projects with the given prefix as 'old project'
 */
boolean oldProject = false

/*
If a project already has a prefix you need to set this value to true to overwrite the existing prefix
 */
boolean overwrite = false

//-----------------------------
// work
assert projectName : "No project name given"

Project.withTransaction {
    Project project = Project.findByName(projectName)
    Project project1 = null
    if (newPrefix) {
        project1 = Project.findByIndividualPrefix(newPrefix)
    } else {
        newPrefix = null
    }

    if (project1 && !oldProject) {
        println "A project with this individual prefix ${newPrefix} already exists. " +
                "Set oldProject to true if you want to reuse this prefix."
        return
    }

    if (project.individualPrefix && !overwrite) {
        println "The prefix ${project.individualPrefix} is set for the selected project ${project.name}. " +
                "If you want to overwrite it with ${newPrefix} set overwrite to true"
    }

    if (overwrite || !project.individualPrefix) {
        if (oldProject && newPrefix) {
            Project.findAllByIndividualPrefix(newPrefix).each {
                it.uniqueIndividualPrefix = !oldProject
                it.save(flush: true)
            }
        }

        project.individualPrefix = newPrefix
        project.uniqueIndividualPrefix = !oldProject
        project.save(flush: true)

        println "changed"

        assert false : "DEBUG: transaction intentionally failed to rollback changes"
    }
}
