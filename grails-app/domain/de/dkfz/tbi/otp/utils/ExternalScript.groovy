package de.dkfz.tbi.otp.utils

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

    String scriptName

    /**
     * Absolut path where the script is located in the file system.
     */
    String location

    String author

    /**
     * Any kind of comment which can come up belonging to the script, like 'what is new', 'why was it changed'...
     */
    String comment

    Date dateCreated

    Date lastUpdated

    Date deprecatedDate

    boolean isDeprecated() {
        return deprecatedDate != null
    }

    @Override
    public String toString() {
        return "external script name: ${scriptName}, path: ${location}, deprecatedDate: ${deprecatedDate}"
    }

    static constraints = {
        //A script name should be unique, but in case of a new version the old version can be set to deprecated by adding a value to deprecatedDate.
        //In this case it is fine to have two entries with the same scriptName.
        scriptName blank: false, validator: { val, obj ->
            if (obj.deprecatedDate) {
                return true
            }
            //I tried to do this in one step like 'findAllByScriptNameAndDeprecatedDateIsNull' but it did not work
            List<ExternalScript> externalScripts = ExternalScript.findAllByScriptName(val)
            List<ExternalScript> externalScriptsNotDeprecated = externalScripts.findAll() {
                it.deprecatedDate == null
            }

            if (externalScriptsNotDeprecated.empty) {
                return true
            } else if (externalScriptsNotDeprecated.size() == 1 && externalScriptsNotDeprecated.contains(obj) ) {
                return true
            } else {
                return false
            }
        }

        location blank: false, validator: { val, obj ->
            return val.startsWith("/")
        }

        author blank: false
        comment blank: true, nullable: true
        deprecatedDate nullable: true
    }
}
