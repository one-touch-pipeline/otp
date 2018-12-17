package de.dkfz.tbi.otp.config

import groovy.transform.TupleConstructor

@TupleConstructor
enum SshAuthMethod {
    SSH_AGENT("sshagent"),
    KEY_FILE("keyfile"),
    PASSWORD("password"),

    final String configName

    static SshAuthMethod getByConfigName(String configName) {
        return values().find { it.configName == configName }
    }
}
