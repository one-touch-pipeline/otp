/*
 * Copyright 2011-2024 The OTP authors
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

/**
 * An overview script to show for all projects the active PI.
 * A PI is active, if he/she is enable for that project and for the whole OTP and has an AD account.
 *
 * Projects without PI are not shown here. For that purpose another script exist.
 */

import de.dkfz.tbi.otp.ngsdata.ProjectRole
import de.dkfz.tbi.otp.ngsdata.UserProjectRole

println "project,account,real name"
println UserProjectRole.withCriteria {
    projections {
        eq('enabled', true)
        projectRoles {
            eq('name', ProjectRole.Basic.PI.name())
        }
        project {
            property('name')
            order('name')
        }
        user {
            property('username')
            order('username')
            property('realName')
            eq('enabled', true)
            isNotNull('username')
        }
    }
}*.join(',').join('\n')
