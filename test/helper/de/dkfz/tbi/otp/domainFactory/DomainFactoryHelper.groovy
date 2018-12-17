package de.dkfz.tbi.otp.domainFactory

import de.dkfz.tbi.otp.ngsdata.DomainFactory

trait DomainFactoryHelper {

    static int getNextId() {
        return DomainFactory.counter++
    }

    def <T> T createDomainObject(Class<T> domainClass, Map defaultProperties, Map parameterProperties, boolean saveAndValidate = true) {
        return DomainFactory.createDomainObject(domainClass, defaultProperties, parameterProperties, saveAndValidate)
    }

    def <T> T findOrCreateDomainObject(Class<T> domainClass, Map defaultProperties, Map parameterProperties, boolean saveAndValidate = true) {
        return DomainFactory.findOrCreateDomainObject(domainClass, defaultProperties, parameterProperties, saveAndValidate)
    }
}
