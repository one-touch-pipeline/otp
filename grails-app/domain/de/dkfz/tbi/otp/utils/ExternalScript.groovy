package de.dkfz.tbi.otp.utils

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

/**
 * The goal is not to write the names or paths of the used scripts in the code, but to store them in the database.
 * This provides more flexibility in cases where the name or paths of the scripts change.
 *
 * Each time when there is a new version of a script (or a completely new script) a new instance should be created in the database
 * and the deprecatedDate of the old version shall be set.
 * If there are little changes like in the comment the current object can be updated.
 *
 */
class ExternalScript {

    /**
     * Identifier which is used in the code. This must stay the same for all script files for one purpose.
     * Otherwise also the code has to be changed.
     */
    String scriptIdentifier

    /**
     * Absolute path of the script file in the file system.
     */
    String filePath

    String author

    /**
     * Any kind of comment which can come up belonging to the script, like 'what is new', 'why was it changed'...
     */
    String comment

    /**
     * This property is handled automatically by grails.
     */
    Date dateCreated

    /**
     * This property is handled automatically by grails.
     */
    Date lastUpdated

    Date deprecatedDate

    boolean isDeprecated() {
        return deprecatedDate != null
    }

    File getScriptFilePath() {
        return new File(filePath)
    }

    @Override
    public String toString() {
        return "external script identifier: ${scriptIdentifier}, path: ${filePath}, deprecatedDate: ${deprecatedDate}"
    }

    static constraints = {
        //A script identifier should be unique, but in case of a new version the old version can be set to deprecated by adding a value to deprecatedDate.
        //In this case it is fine to have two entries with the same scriptIdentifier.
        scriptIdentifier blank: false, validator: { val, obj ->
            if (obj.deprecatedDate) {
                return true
            }
            //I tried to do this in one step like 'findAllByScriptIdentifierAndDeprecatedDateIsNull' but it did not work
            List<ExternalScript> externalScripts = ExternalScript.findAllByScriptIdentifier(val)
            List<ExternalScript> externalScriptsNotDeprecated = externalScripts.findAll() { it.deprecatedDate == null }

            if (externalScriptsNotDeprecated.empty) {
                return true
            } else if (externalScriptsNotDeprecated.size() == 1 && externalScriptsNotDeprecated.contains(obj) ) {
                return true
            } else {
                return false
            }
        }

        filePath blank: false, unique: true, validator: { val, obj ->
            return new File(val).isAbsolute()
        }
        author blank: false
        comment blank: true, nullable: true
        deprecatedDate nullable: true
    }

    static mapping = {
        scriptIdentifier index: "external_script_identifier_idx"
    }

    static ExternalScript getLatestVersionOfScript(final String scriptIdentifier) {
        return exactlyOneElement(ExternalScript.findAllByScriptIdentifierAndDeprecatedDateIsNull(scriptIdentifier))
    }
}
