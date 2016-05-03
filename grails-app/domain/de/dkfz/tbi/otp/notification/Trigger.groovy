package de.dkfz.tbi.otp.notification

import de.dkfz.tbi.otp.utils.Entity

/**
 * A Trigger represents a Domain object for which Notifications are sent.
 * E.g. a JobExecutionPlan and it's Id.
 *
 */
class Trigger implements Entity {
    /**
     * The Class for the Trigger.
     */
    TriggerClass clazz
    /**
     * The Domain Object id.
     */
    long triggerId
    /**
     * An optional ACL protection for the trigger. This is used in conjunction
     * with a {@link SidRecipient}. If this field is set the SidRecipient will only
     * receive the notification if and only if he has read permission on the object
     * identified by this field from the ProcessParameter.
     *
     * For example: the ProcessParameter references a Run. The aclField is set to
     * "seqCenter", so the ACL check tests whether the SidRecipient has read access
     * to the Run's SeqCenter.
     *
     * If this field has the value "this" instead of traversing for the object to check
     * the ProcessParameter's object is used directly. E.g. in the example above the
     * check would be done on the Run itself.
     */
    String aclField

    static constraints = {
        triggerId(unique: ['clazz', 'aclField'])
        aclField(nullable: true, blank: true)
    }

    /**
     * Retrieves the domain Object this Trigger is for.
     * @return
     */
    def toTriggerObject() {
        return Trigger.executeQuery("FROM " + clazz.className + " WHERE id=" + triggerId).first()
    }
}
