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

import grails.gorm.hibernate.annotation.ManagedEntity
import org.apache.commons.lang.builder.HashCodeBuilder

import de.dkfz.tbi.otp.security.user.RolesService
import de.dkfz.tbi.otp.utils.Entity

/**
 * Auto generated class by spring security plugin.
 */
@ManagedEntity
class UserRole implements Serializable, Entity {

    User user
    Role role

    @Override
    boolean equals(Object other) {
        if (!(other instanceof UserRole)) {
            return false
        }

        return other.user?.id == user?.id &&
                other.role?.id == role?.id
    }

    @Override
    int hashCode() {
        def builder = new HashCodeBuilder()
        if (user) {
            builder.append(user.id)
        }
        if (role) {
            builder.append(role.id)
        }
        return builder.toHashCode()
    }

    static UserRole get(long userId, long roleId) {
        return find('from UserRole where user.id=:userId and role.id=:roleId',
                [userId: userId, roleId: roleId])
    }

    static UserRole create(User user, Role role) {
        return RolesService.createUserRole(user, role)
    }

    static void removeAll(User user) {
        executeUpdate 'DELETE FROM UserRole WHERE user=:user', [user: user]
    }

    static void removeAll(Role role) {
        executeUpdate 'DELETE FROM UserRole WHERE role=:role', [role: role]
    }

    static mapping = {
        id composite: ['role', 'user']
        version false
    }
}
