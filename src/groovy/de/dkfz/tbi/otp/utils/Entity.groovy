package de.dkfz.tbi.otp.utils

import static org.hibernate.proxy.HibernateProxyHelper.getClassWithoutInitializingProxy

trait Entity {

    @Override
    int hashCode() {
        return id?.hashCode() ?: super.hashCode()
    }

    @Override
    boolean equals(Object other) {
        if (this.is(other)) {
            return true
        }
        Class thisClass = getClassWithoutInitializingProxy(this)
        Class otherClass = getClassWithoutInitializingProxy(other)
        return (thisClass == otherClass || thisClass.isAssignableFrom(otherClass) || otherClass.isAssignableFrom(thisClass)) &&
                this.id && other?.id == this.id
    }
}
