package de.dkfz.tbi.otp.notification

import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclSid

/**
 * Concrete Recipient to use an AclSid as recipient.
 * If a user sid is configured the user will be used, if
 * a role is configured all users with the role will be used.
 *
 *
 */
class SidRecipient extends Recipient {
    AclSid sid

    static mapping = {
        version(false)
    }

    static constraints = {
        sid(nullable: false, unique: true)
    }
}
