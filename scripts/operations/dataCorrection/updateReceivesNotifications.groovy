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

import de.dkfz.tbi.otp.ngsdata.UserProjectRole
import de.dkfz.tbi.otp.security.User

//************ Username of user  ************//
String username = ""

//************ set receives Notifications (true/false) ************//
Boolean receivesNotifications = false

//************ (optional) List of project names  ************//
List projectNames = """
#projectName1
#projectName2


""".split("\n")*.trim().findAll {
    it && !it.startsWith('#')
}


User.withTransaction {
    User user = User.findByUsername(username)
    List<UserProjectRole> userProjectRoles = []

    if (projectNames) {
        userProjectRoles = UserProjectRole.createCriteria().list {
            eq('user', user)
            and {
                project {
                    'in'('name', projectNames)
                }
            }
        }
    } else {
        userProjectRoles = UserProjectRole.findAllByUser(user)
    }

    userProjectRoles.each { UserProjectRole userProjectRole ->
        println "************"
        println userProjectRole.project
        userProjectRole.receivesNotifications = receivesNotifications
        userProjectRole.save(flush: true)
        println "receives notifications set to ${receivesNotifications}"
    }

    assert false: "DEBUG: transaction intentionally failed to rollback changes"
}

""