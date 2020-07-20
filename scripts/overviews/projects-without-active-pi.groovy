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

/**
 * A script to projects without an active PI.
 * A PI is active, if he/she is enable for that project and for the whole OTP and has an AD account.
 */

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

List<Long> projectIds = UserProjectRole.withCriteria {
    projections {
        eq('enabled', true)
        projectRoles {
            eq('name', ProjectRole.Basic.PI.name())
        }
        project {
            property('id')
        }
        user {
            eq('enabled', true)
            isNotNull('username')
        }
    }
}

println((projectIds ? Project.withCriteria {
    not {
        'in'('id', projectIds)
    }
} : Project.list())*.name.sort().join('\n'))
