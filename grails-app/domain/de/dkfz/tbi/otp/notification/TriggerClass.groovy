package de.dkfz.tbi.otp.notification

/**
 * Simple wrapper around a class name to not have a String in the Trigger class.
 * @see Trigger
 */
class TriggerClass {

    String className

    static mapping = {
        version(false)
    }

    static constraints = {
        className(unique: true, blank: false, size: 1..255)
    }
}
