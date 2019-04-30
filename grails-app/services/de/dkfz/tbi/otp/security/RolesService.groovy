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

package de.dkfz.tbi.otp.security

import grails.gorm.transactions.Transactional
import org.springframework.security.access.prepost.PreAuthorize

@Transactional
class RolesService {

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<RolesWithUsers> getRolesAndUsers() {
        List<RolesWithUsers> roles = Role.list().collect { Role role ->
            new RolesWithUsers(role: role)
        }
        roles.each {
            it.users = UserRole.findAllByRole(it.role)*.user.flatten()
            it.users.sort { User user -> user.username }
        }
        roles.sort { it.role.authority }
        return roles
    }

    static void createUserRole(User user, Role role) {
        new UserRole(user: user, role: role).save(flush: true, insert: true)
    }
}

class RolesWithUsers {
    Role role
    List<User> users = []
}
