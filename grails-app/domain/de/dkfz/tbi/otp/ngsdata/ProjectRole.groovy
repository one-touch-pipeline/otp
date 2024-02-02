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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.utils.Entity

/** This table is used externally. Please discuss a change in the team */
@ManagedEntity
class ProjectRole implements Entity {

    /**
     * The set of essential ProjectRoles.
     *
     * These project roles are required to exist and are seeded into the database.
     * The names of the enum values have to be an exact match to the way they are
     * stored in the database.
     * It is possible and common to add more ProjectRoles than the ones given here.
     *
     * Other roles, that are not listed in Basic are not enforced and should be
     * used exclusively for informational purposes.
     *
     * Do not assign any kind of behaviour to roles other than the Basic ProjectRoles.
     *
     * All Basic ProjectRoles should be created as default values in this script:
     * changelogs/defaultValues/project-roles.sql
     */
    static enum Basic {
        COORDINATOR,
        OTHER,
        PI,
        LEAD_BIOINFORMATICIAN,
        BIOINFORMATICIAN,
        SUBMITTER,

        static List<Basic> asList() {
            return values() as List<Basic>
        }
    }

    static final List<String> ALL_BASIC_PROJECT_ROLES = asImmutableStringList(Basic.asList())

    /**
     * Roles given to users that are authority figures for the project.
     *
     * This means they are eligible to be contacted for questions regarding the project
     * and can be taken as a trustworthy source, regarding the management, direction
     * and legal paperwork of the project. They are to be notified about changes to
     * the mentioned topics.
     */
    static final List<String> AUTHORITY_PROJECT_ROLES = asImmutableStringList([
            Basic.PI,
    ])

    static final List<String> BIOINFORMATICIAN_PROJECT_ROLES = asImmutableStringList([
            Basic.LEAD_BIOINFORMATICIAN,
            Basic.BIOINFORMATICIAN,
    ])

    /** This attribute is used externally. Please discuss a change in the team */
    String name

    static constraints = {
        name(blank: false, unique: true)
    }

    private static List<String> asImmutableStringList(List<Basic> list) {
        return list*.toString().asImmutable()
    }

    @Override
    String toString() {
        return name
    }
}
