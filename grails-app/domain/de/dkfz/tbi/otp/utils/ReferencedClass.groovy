package de.dkfz.tbi.otp.utils

/**
 * Helper class for referencing another domain class by name.
 */
class ReferencedClass implements Entity {
    String className

    static constraints = {
        className(unique: true)
    }
}
