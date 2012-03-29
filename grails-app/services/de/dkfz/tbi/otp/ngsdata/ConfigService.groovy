package de.dkfz.tbi.otp.ngsdata


/**
 * This service knows all the configuration parameters like root paths,
 * hosts names, user ids
 * 
 *
 */
class ConfigService {

    String getProjectRootPath(Project proj) {
        String host = proj.host
        switch (host) {
            case "BioQuant":
                return "$ROOT_PATH/project/"
            case "DKFZ":
                return "$ROOT_PATH/project/"
            default:
                throw new Exception()
        }
    }

    String getProjectSequencePath(Project proj) {
        String base = getProjectRootPath(proj)
        return "${base}/${proj.dirName}/sequencing/"
    }

    String igvPath() {
        return "http://www.broadinstitute.org/igv/projects/current/igv.php?sessionURL="
    }

    String dataWebServer() {
        return "https://otp.local/"
    }

    String otpWebServer() {
        return "https://otp.local/otpdevel/"
    }
}
