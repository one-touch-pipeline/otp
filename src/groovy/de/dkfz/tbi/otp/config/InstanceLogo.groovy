package de.dkfz.tbi.otp.config

import groovy.transform.*

@TupleConstructor
enum InstanceLogo {
    NONE("header-empty.png"),
    CHARITE("header-charite.png"),
    DKFZ("header-dkfz.png"),

    final String fileName

    static InstanceLogo findByName(String name) {
        return values().find {
            it.name() == name
        }
    }
}
