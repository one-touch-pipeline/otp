package de.dkfz.tbi.otp.utils

/**
 * Helper for comparing domain ids and calculate hashcode.
 * TODO: After grails update use more generic solution, see OTP-1710
 */
class IdCompareHelper {

    static boolean equals(def domain1, def domain2) {
        if (domain1 == null) {
            return domain2 == null
        }
        if (domain1.is(domain2)) {
            return true
        }

        return domain1.id != null && domain1.id == domain2.id && domain1.getClass() == domain2.getClass()
    }

    static int hashCode(def domain) {
        return (domain?.id != null ? domain.id.hashCode() : 0)
    }
}
