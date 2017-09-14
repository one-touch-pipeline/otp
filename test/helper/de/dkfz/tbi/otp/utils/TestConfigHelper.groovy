package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.*
import org.codehaus.groovy.grails.commons.*

class TestConfigHelper {

    static String testingGroup(GrailsApplication grailsApplication) {
        String testingGroup = grailsApplication.config.otp.testing.group
        assert testingGroup: '"otp.testing.group" is not set in your "otp.properties". Please add it with an existing secondary group.'
        assert testingGroup != TestCase.primaryGroup(): '"otp.testing.group" does not differ from your primary group'
        return testingGroup
    }

    /**
     * The account name to use on the DKFZ cluster. This can be overwritten by the key
     * <code>otp.testing.workflows.account</code> in the configuration file.
     *
     * @return the account name set in the configuration file, or the default account name otherwise.
     */
    static String getWorkflowTestAccountName(GrailsApplication grailsApplication) {
        return grailsApplication.config.otp.testing.workflows.account
    }

    static File getWorkflowTestRootDir(GrailsApplication grailsApplication) {
        return new File(grailsApplication.config.otp.testing.workflows.rootdir)

    }

}
