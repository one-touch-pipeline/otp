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

import grails.plugin.springsecurity.acl.AclClass
import grails.plugin.springsecurity.acl.AclEntry
import grails.plugin.springsecurity.acl.AclObjectIdentity


/** Script to find missing ACL object identities and entries */
AclClass.all.each { AclClass aclClass ->
    Class clazz = grailsApplication.getDomainClass(aclClass.className).clazz
    println aclClass.className

    clazz.all.each {
        println "  * ${it.class.simpleName} ${it.id}"


        AclObjectIdentity objectIdentity = AclObjectIdentity.findByAclClassAndObjectId(aclClass, it.id as long)

        if(!objectIdentity) {
            println "### no OI for ${it.className} ${it.id}"
        } else {
            List<AclEntry> aclEntries = AclEntry.findAllByAclObjectIdentity(objectIdentity)

            if(aclEntries.empty) {
                println "### no AE for ${objectIdentity}"
            }
        }
    }
}
[]
