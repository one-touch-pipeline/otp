/*
 * Copyright 2011-2020 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.config

import grails.util.Environment

enum OtpProperty {
    OIDC_ENABLED('otp.security.oidc.enabled', TypeValidators.BOOLEAN, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT), 'false'),

    LDAP_ENABLED('otp.security.ldap.enabled', TypeValidators.BOOLEAN, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT), 'true'),
    LDAP_SERVER('otp.security.ldap.server', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),
    LDAP_MANAGER_DN('otp.security.ldap.managerDn', TypeValidators.SINGLE_LINE_TEXT_OPTIONAL, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),
    LDAP_MANAGER_PASSWORD('otp.security.ldap.managerPw', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),
    LDAP_SEARCH_BASE('otp.security.ldap.search.base', TypeValidators.SINGLE_LINE_TEXT_OPTIONAL, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),
    LDAP_SEARCH_ATTRIBUTE('otp.security.ldap.search.attribute', TypeValidators.SINGLE_LINE_TEXT_OPTIONAL, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),
    TRUSTSTORE_PATH("otp.trustStore.path", TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT), '/'),
    TRUSTSTORE_PASSWORD("otp.trustStore.password", TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT), 'invalid'),
    TRUSTSTORE_TYPE("otp.trustStore.type", TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT), 'jks'),

    KEYCLOAK_SERVER('otp.security.keycloak.server', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.DEVELOPMENT)),
    KEYCLOAK_REALM('otp.security.keycloak.realm', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.DEVELOPMENT)),
    KEYCLOAK_CLIENT_ID('otp.security.keycloak.clientId', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.DEVELOPMENT)),
    KEYCLOAK_CLIENT_SECRET('otp.security.keycloak.clientSecret', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.DEVELOPMENT)),

    GRAILS_CONSOLE('otp.security.console.enabled', TypeValidators.BOOLEAN, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT), 'false'),

    SSH_AUTH_METHOD('otp.ssh.authMethod', TypeValidators.SSH_AUTH_METHOD, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT, UsedIn.WORKFLOW_TEST),
            SshAuthMethod.SSH_AGENT.name()),
    SSH_USER('otp.ssh.user', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),
    /**
     * Used for {@link SshAuthMethod#KEY_FILE}
     */
    SSH_KEY_FILE('otp.ssh.keyFile', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT, UsedIn.WORKFLOW_TEST),
            System.getProperty("user.home") + "/.ssh/id_rsa"),
    /**
     * Used for {@link SshAuthMethod#PASSWORD}
     */
    SSH_PASSWORD('otp.ssh.password', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT, UsedIn.WORKFLOW_TEST), "invalid"),

    PATH_STACK_TRACES('otp.errorLogging.stacktraces', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT),
            "${System.getProperty("user.dir")}/logs/stacktraces/"),
    PATH_JOB_LOGS('otp.logging.jobLogDir', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT),
            "${System.getProperty("user.dir")}/logs/jobs/"),
    PATH_PROCESSING_ROOT('otp.processing.root.path', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.PRODUCTION)),
    PATH_PROJECT_ROOT('otp.root.path', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.PRODUCTION, UsedIn.DEVELOPMENT)),
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

    CONFIG_EMAIL_ENABLED('otp.mail.allowOtpToSendMails', TypeValidators.BOOLEAN, EnumSet.of(UsedIn.PRODUCTION), 'false'),
    CONFIG_EMAIL_SERVER('otp.mail.server', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION), 'localhost'),
    CONFIG_EMAIL_PORT('otp.mail.port', TypeValidators.POSITIVE_NUMBER, EnumSet.of(UsedIn.PRODUCTION), '25'),
    CONFIG_EMAIL_USERNAME('otp.mail.username', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION), ''),
    CONFIG_EMAIL_PASSWORD('otp.mail.password', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION), ''),

    WES_AUTH_BASE_URL('otp.wes.auth.baseUrl', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.WORKFLOW_TEST, UsedIn.DEVELOPMENT)),
    WES_AUTH_CLIENT_ID('otp.wes.auth.clientId', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION, UsedIn.WORKFLOW_TEST, UsedIn.DEVELOPMENT)),
    WES_AUTH_CLIENT_SECRET('otp.wes.auth.clientSecret', TypeValidators.SINGLE_WORD_TEXT,
            EnumSet.of(UsedIn.PRODUCTION, UsedIn.WORKFLOW_TEST, UsedIn.DEVELOPMENT)),

    CONFIG_JOB_SYSTEM_START('otp.jobsystem.start', TypeValidators.BOOLEAN, EnumSet.of(UsedIn.PRODUCTION), 'false'),
    CONFIG_SERVER_URL('otp.server.url', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.PRODUCTION)),
    CONFIG_ENVIRONMENT_NAME('otp.environment.name', TypeValidators.SINGLE_LINE_TEXT_OPTIONAL, EnumSet.of(UsedIn.PRODUCTION), null),
    CONFIG_AUTO_IMPORT_SECRET('otp.autoimport.secret', TypeValidators.SINGLE_LINE_TEXT_OPTIONAL, EnumSet.of(UsedIn.PRODUCTION), ''),

    CONFIG_DICOM_INSTANCE_NAME('dicom.instance.name', TypeValidators.SINGLE_LINE_TEXT_OPTIONAL, EnumSet.of(UsedIn.PRODUCTION)),
    CONFIG_DICOM_INSTANCE_ID('dicom.instance.id', TypeValidators.POSITIVE_NUMBER, EnumSet.of(UsedIn.PRODUCTION), "1"),

    TEST_TESTING_GROUP('otp.testing.group', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.WORKFLOW_TEST, UsedIn.TEST)),
    TEST_TESTING_PROJECT_UNIX_GROUP('otp.testing.project.unix.group', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.WORKFLOW_TEST, UsedIn.TEST)),
    TEST_WORKFLOW_ACCOUNT('otp.testing.workflows.account', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.WORKFLOW_TEST)),
    TEST_WORKFLOW_SCHEDULER('otp.testing.workflows.scheduler', TypeValidators.JOB_SCHEDULER, EnumSet.of(UsedIn.WORKFLOW_TEST)),
    TEST_WORKFLOW_HOST('otp.testing.workflows.host', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.WORKFLOW_TEST)),
    /**
     * Directory holding the reference data for workflow tests
     */
    TEST_WORKFLOW_INPUT_DIR('otp.testing.workflows.input', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.WORKFLOW_TEST)),
    /**
     * Base Directory for running the workflow test. Each test creates inside its own sub directory.
     */
    TEST_WORKFLOW_RESULT_DIR('otp.testing.workflows.result', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.WORKFLOW_TEST)),
    TEST_WORKFLOW_RODDY_SHARED_FILES_BASE_DIRECTORY('otp.testing.workflows.roddy.sharedFiles', TypeValidators.ABSOLUTE_PATH, EnumSet.of(UsedIn.WORKFLOW_TEST)),
    TEST_WORKFLOW_QUEUE('otp.testing.workflows.queue', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.WORKFLOW_TEST)),
    TEST_WORKFLOW_CONFIG_SUFFIX('otp.testing.workflows.config.suffix', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.WORKFLOW_TEST)),
    TEST_WORKFLOW_FASTTRACK_QUEUE('otp.testing.workflows.roddy.fasttrack.queue', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.WORKFLOW_TEST)),
    TEST_WORKFLOW_FASTTRACK_CONFIG_SUFFIX('otp.testing.workflows.config.fasttrack.suffix', TypeValidators.SINGLE_WORD_TEXT, EnumSet.of(UsedIn.WORKFLOW_TEST)),

    final String key

    final TypeValidators validator

    /**
     * In which environments is this property <em>required</em>?
     *
     * @see PropertiesValidationService
     */
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

    static OtpProperty getByKey(String key) {
        return values().find {
            it.key == key
        }
    }
}

/**
 * Potential Environments for OTP execution.
 *
 * Closely aligned, but not identical with, {@link Environment}, as we treat "workflow tests" as a pseudo-environment.
 */
enum UsedIn {
    DEVELOPMENT,
    PRODUCTION,
    TEST,
    WORKFLOW_TEST,
}
