package de.dkfz.tbi.otp.config

import grails.util.*

enum OtpProperty {

    LDAP_ENABLED('otp.security.ldap.enabled', TypeValidators.BOOLEAN, 'true'),
    LDAP_SERVER('otp.security.ldap.server', TypeValidators.SINGLE_WORD_TEXT),
    LDAP_MANAGER_DN('otp.security.ldap.managerDn', TypeValidators.SINGLE_LINE_TEXT),
    LDAP_MANAGER_PASSWORD('otp.security.ldap.managerPw', TypeValidators.SINGLE_WORD_TEXT),
    LDAP_SEARCH_BASE('otp.security.ldap.search.base', TypeValidators.SINGLE_LINE_TEXT),
    LDAP_SEARCH_SUBTREE('otp.security.ldap.search.subTree', TypeValidators.BOOLEAN, 'true'),
    LDAP_SEARCH_FILTER('otp.security.ldap.search.filter', TypeValidators.SINGLE_LINE_TEXT),

    SSH_AUTH_METHOD('otp.ssh.authMethod', TypeValidators.SSH_AUTH_METHOD, SshAuthMethod.SSH_AGENT.name()),
    SSH_USER('otp.ssh.user', TypeValidators.SINGLE_WORD_TEXT),
    /**
     * Used for {@link SshAuthMethod#KEY_FILE}
     */
    SSH_KEY_FILE('otp.ssh.keyFile', TypeValidators.ABSOLUTE_PATH, System.getProperty("user.home") + "/.ssh/id_rsa"),
    /**
     * Used for {@link SshAuthMethod#PASSWORD}
     */
    SSH_PASSWORD('otp.ssh.password', TypeValidators.SINGLE_WORD_TEXT),

    PATH_STACK_TRACES('otp.errorLogging.stacktraces', TypeValidators.ABSOLUTE_PATH, "logs/stacktraces/"),
    PATH_JOB_LOGS('otp.logging.jobLogDir', TypeValidators.ABSOLUTE_PATH, "logs/jobs/"),
    PATH_PROCESSING_ROOT('otp.processing.root.path', TypeValidators.ABSOLUTE_PATH),
    PATH_PROJECT_ROOT('otp.root.path', TypeValidators.ABSOLUTE_PATH),
    PATH_CLUSTER_LOGS_OTP('otp.logging.root.path', TypeValidators.ABSOLUTE_PATH),
    PATH_TOOLS('otp.path.tools', TypeValidators.ABSOLUTE_PATH),
    PATH_RODDY('otp.path.tools.roddy', TypeValidators.ABSOLUTE_PATH),
    PATH_SEQ_CENTER_INBOX('otp.path.seqCenterInbox', TypeValidators.ABSOLUTE_PATH),
    PATH_SCRIPTS('otp.path.script.root', TypeValidators.ABSOLUTE_PATH),

    DATABASE_SERVER('otp.database.server', TypeValidators.SINGLE_WORD_TEXT),
    DATABASE_PORT('otp.database.port', TypeValidators.POSITIVE_NUMBER),
    DATABASE_SCHEMA('otp.database.database', TypeValidators.SINGLE_WORD_TEXT),
    DATABASE_USERNAME('otp.database.username', TypeValidators.SINGLE_WORD_TEXT),
    DATABASE_PASSWORD('otp.database.password', TypeValidators.SINGLE_WORD_TEXT),
    DATABASE_CREATE('otp.database.dbCreate', TypeValidators.SINGLE_WORD_TEXT),

    CONFIG_ACTIVATE_EMAIL('otp.mail.allowOtpToSendMails', TypeValidators.BOOLEAN, 'false'),
    CONFIG_JOB_SYSTEM_START('otp.jobsystem.start', TypeValidators.BOOLEAN, 'false'),
    CONFIG_SERVER_URL('otp.server.url', TypeValidators.SINGLE_WORD_TEXT),
    CONFIG_ENVIRONMENT_NAME('otp.environment.name', TypeValidators.SINGLE_LINE_TEXT, Environment.getCurrent().name),

    DEVEL_USE_BACKDOOR('otp.security.useBackdoor', TypeValidators.BOOLEAN),
    DEVEL_BACKDOOR_USER('otp.security.backdoorUser', TypeValidators.SINGLE_WORD_TEXT),

    TEST_TESTING_GROUP('otp.testing.group', TypeValidators.SINGLE_WORD_TEXT),
    TEST_WORKFLOW_ACCOUNT('otp.testing.workflows.account', TypeValidators.SINGLE_WORD_TEXT),
    TEST_WORKFLOW_SCHEDULER('otp.testing.workflows.scheduler', TypeValidators.JOB_SCHEDULER),
    TEST_WORKFLOW_HOST('otp.testing.workflows.host', TypeValidators.SINGLE_WORD_TEXT),
    TEST_WORKFLOW_ROOTDIR('otp.testing.workflows.rootdir', TypeValidators.ABSOLUTE_PATH)



    final String key

    final TypeValidators validator

    final boolean provideDefault

    final String defaultValue


    OtpProperty(String key, TypeValidators validator) {
        assert key
        assert validator

        this.key = key
        this.validator = validator
        this.provideDefault = false
        this.defaultValue = null
    }

    OtpProperty(String key, TypeValidators validator, String defaultValue) {
        assert key
        assert validator

        this.key = key
        this.validator = validator
        this.provideDefault = true
        this.defaultValue = defaultValue
    }

    boolean hasDefaultValue() {
        return provideDefault
    }

    static OtpProperty findByKey(String key) {
        return values().find {
            it.key == key
        }
    }

}
