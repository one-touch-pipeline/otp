package de.dkfz.tbi.otp.config

import grails.util.*

enum OtpProperty {

    LDAP_ENABLED('otp.security.ldap.enabled', TypeValidators.BOOLEAN, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT), 'true'),
    LDAP_SERVER('otp.security.ldap.server', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),
    LDAP_MANAGER_DN('otp.security.ldap.managerDn', TypeValidators.SINGLE_LINE_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),
    LDAP_MANAGER_PASSWORD('otp.security.ldap.managerPw', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),
    LDAP_SEARCH_BASE('otp.security.ldap.search.base', TypeValidators.SINGLE_LINE_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),
    LDAP_SEARCH_SUBTREE('otp.security.ldap.search.subTree', TypeValidators.BOOLEAN, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT), 'true'),
    LDAP_SEARCH_FILTER('otp.security.ldap.search.filter', TypeValidators.SINGLE_LINE_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),

    SSH_AUTH_METHOD('otp.ssh.authMethod', TypeValidators.SSH_AUTH_METHOD, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT, UsedIn.WORKFLOW_TEST), SshAuthMethod.SSH_AGENT.name()),
    SSH_USER('otp.ssh.user', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),
    /**
     * Used for {@link SshAuthMethod#KEY_FILE}
     */
    SSH_KEY_FILE('otp.ssh.keyFile', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT, UsedIn.WORKFLOW_TEST), System.getProperty("user.home") + "/.ssh/id_rsa"),
    /**
     * Used for {@link SshAuthMethod#PASSWORD}
     */
    SSH_PASSWORD('otp.ssh.password', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT, UsedIn.WORKFLOW_TEST), "invalid"),

    PATH_STACK_TRACES('otp.errorLogging.stacktraces', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.PRODUCTION)),
    PATH_JOB_LOGS('otp.logging.jobLogDir', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.PRODUCTION)),
    PATH_PROCESSING_ROOT('otp.processing.root.path', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.PRODUCTION)),
    PATH_PROJECT_ROOT('otp.root.path', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.PRODUCTION)),
    PATH_CLUSTER_LOGS_OTP('otp.logging.root.path', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),
    PATH_TOOLS('otp.path.tools', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.PRODUCTION, UsedIn.WORKFLOW_TEST)),
    PATH_RODDY('otp.path.tools.roddy', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.PRODUCTION, UsedIn.WORKFLOW_TEST)),
    PATH_SEQ_CENTER_INBOX('otp.path.seqCenterInbox', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),
    PATH_SCRIPTS_OUTPUT('otp.path.script.root', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.PRODUCTION)),

    DATABASE_SERVER('otp.database.server', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),
    DATABASE_PORT('otp.database.port', TypeValidators.POSITIVE_NUMBER, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),
    DATABASE_SCHEMA('otp.database.database', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),
    DATABASE_USERNAME('otp.database.username', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),
    DATABASE_PASSWORD('otp.database.password', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),

    CONFIG_ACTIVATE_EMAIL('otp.mail.allowOtpToSendMails', TypeValidators.BOOLEAN, EnumSet.of(UsedIn.PRODUCTION), 'false'),
    CONFIG_JOB_SYSTEM_START('otp.jobsystem.start', TypeValidators.BOOLEAN, EnumSet.of(UsedIn.PRODUCTION), 'false'),
    CONFIG_SERVER_URL('otp.server.url', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION)),
    CONFIG_ENVIRONMENT_NAME('otp.environment.name', TypeValidators.SINGLE_LINE_TEXT, EnumSet.of(UsedIn.PRODUCTION), Environment.getCurrent().name),

    DEVEL_USE_BACKDOOR('otp.security.useBackdoor', TypeValidators.BOOLEAN, EnumSet.of(UsedIn.DEVELOPMENT), "false"),
    DEVEL_BACKDOOR_USER('otp.security.backdoorUser', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.DEVELOPMENT)),

    TEST_TESTING_GROUP('otp.testing.group', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.WORKFLOW_TEST, UsedIn.TEST)),
    TEST_WORKFLOW_ACCOUNT('otp.testing.workflows.account', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.WORKFLOW_TEST)),
    TEST_WORKFLOW_SCHEDULER('otp.testing.workflows.scheduler', TypeValidators.JOB_SCHEDULER, EnumSet.of(UsedIn.WORKFLOW_TEST)),
    TEST_WORKFLOW_HOST('otp.testing.workflows.host', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.WORKFLOW_TEST)),
    TEST_WORKFLOW_ROOTDIR('otp.testing.workflows.rootdir', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.WORKFLOW_TEST))



    final String key

    final TypeValidators validator

    EnumSet<UsedIn> usedIn

    final String defaultValue


    OtpProperty(String key, TypeValidators validator, EnumSet<UsedIn> usedIn, String defaultValue = null) {
        assert key
        assert validator
        assert usedIn

        this.key = key
        this.validator = validator
        this.usedIn = usedIn
        this.defaultValue = defaultValue
    }

    static OtpProperty findByKey(String key) {
        return values().find {
            it.key == key
        }
    }
}

enum UsedIn {
    DEVELOPMENT,
    PRODUCTION,
    TEST,
    WORKFLOW_TEST,
}
