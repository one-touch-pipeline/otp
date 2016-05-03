package de.dkfz.tbi.otp.utils

import static org.hibernate.proxy.HibernateProxyHelper.*

trait Entity {

    @Override
    int hashCode() {
        return id?.hashCode() ?: super.hashCode()
    }

    @Override
    boolean equals(Object other) {
        return this.is(other) ||
                getClassWithoutInitializingProxy(this) == getClassWithoutInitializingProxy(other) &&
                this.id && other?.id == this.id
    }
}
