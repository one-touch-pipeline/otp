package de.dkfz.tbi.otp.notification

import de.dkfz.tbi.otp.security.User
import org.springframework.beans.factory.InitializingBean
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Message.Type

/**
 * Simple service to send XMPP messages.
 *
 */
class JabberService implements InitializingBean {
    /**
     * Dependency injection of grails application
     */
    @SuppressWarnings("GrailsStatelessService")
    def grailsApplication
    /**
     * Dependency injected Jabber service name
     */
    @SuppressWarnings("GrailsStatelessService")
    String service
    /**
     * Dependency injected user name
     */
    @SuppressWarnings("GrailsStatelessService")
    String username
    /**
     * Dependency injected password
     */
    @SuppressWarnings("GrailsStatelessService")
    String password
    /**
     * Internal connection to jabber server
     */
    @SuppressWarnings("GrailsStatelessService")
    private XMPPConnection xmppConnection = null

    public void afterPropertiesSet() {
        if (!grailsApplication.config.otp.jabber.enabled) {
            return
        }
        ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration(service)
        connectionConfiguration.selfSignedCertificateEnabled = true
        connectionConfiguration.reconnectionAllowed = true
        xmppConnection = new XMPPConnection(connectionConfiguration)
        xmppConnection.connect()
        xmppConnection.login(username, password)
    }

    /**
     * Sends a XMPP message to the User if the User has a Jabber Id configured.
     * @param user The User to whom the message should be sent
     * @param text The message body
     */
    void sendNotification(User user, String text) {
        if (!xmppConnection) {
            return
        }
        if (!user.jabberId) {
            return
        }
        Message msg = new Message(user.jabberId, Type.normal)
        msg.setBody(text)
        xmppConnection.sendPacket(msg)
    }
}
