import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclClass
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclEntry
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclObjectIdentity


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
