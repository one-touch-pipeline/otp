/*
 * Copyright 2011-2020 The OTP authors
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

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User

/**
 * Lists all enabled and active users with name and email of active projects.
 *
 * Active projects are projects, that received data in the given time frame. The end-date of the time
 * frame is fixed to the present date, but the start point can be freely configured via the variables
 * yearStart, monthStart, dayStart.
 * The format for the user output can be configured via the userFormatter closure.
 */

// Config Area
def (yearStart, monthStart, dayStart) = [2018, 1, 1]

Closure<String> userFormatter = { User user ->
    return "${user.username ?: ""},${user.realName},${user.email}"
}

// Execution Area
Date start = new Date(yearStart - 1900, monthStart - 1, dayStart)
Date end = new Date()

Set<Project> projects = [] as Set<Project>;
Set<User> users = [] as Set<User>;

DataFile.createCriteria().listDistinct {
    between("dateCreated", start, end)
    projections {
        seqTrack {
            sample {
                individual {
                    property("project")
                }
            }
        }
    }
}.each { Project p ->
    projects.add(p)
    users.addAll(UserProjectRole.findAllByProjectAndEnabled(p, true)*.user)
}

Set<User> filteredUsers = users.findAll { it.enabled } as Set<User>;

// Output Area
println """\
# Source Projects:
${projects*.name.sort().join("\n")}

# Users:
${filteredUsers.collect(userFormatter).sort().join("\n")}
"""

''
