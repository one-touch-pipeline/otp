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
package de.dkfz.tbi.otp.administration

import grails.validation.Validateable

import de.dkfz.tbi.otp.security.Group
import de.dkfz.tbi.otp.security.Role

/**
 * Command object to perform data binding for creation of a Group.
 *
 * For further documentation about the various available fields,
 * please see documentation of Group.
 *
 * @see Group
 */
class GroupCommand implements Validateable {
    String name
    String description
    boolean readProject
    boolean writeProject
    boolean readJobSystem
    boolean writeJobSystem
    boolean readSequenceCenter
    boolean writeSequenceCenter

    static constraints = {
        name(blank: false, validator: { String value ->
            if (Role.findByAuthority("GROUP_" + value.toUpperCase().replace(' ', '_'))) {
                return "Group already exists."
            }
        })
        description(blank: true)
        writeProject(validator: { boolean value, GroupCommand object ->
            if (!object.readProject && value) {
                return 'Write without read'
            }
        })
        writeJobSystem(validator: { boolean value, GroupCommand object ->
            if (!object.readJobSystem && value) {
                return 'Write without read'
            }
        })
        writeSequenceCenter(validator: { boolean value, GroupCommand object ->
            if (!object.readSequenceCenter && value) {
                return 'Write without read'
            }
        })
    }
}
