package de.dkfz.tbi.otp.dataprocessing

import java.util.regex.Pattern

import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.Realm.OperationType

/**
 * Represents a relative file system path.
 */
class OtpPath {

    final Project project
    final File relativePath

    OtpPath(final Project project, final String first, final String... more) {
        this.project = project
        relativePath = LsdfFilesService.getPath(first, more)
        assert !relativePath.absolute
    }

    OtpPath(final OtpPath path, final String first, final String... more) {
        project = path.project
        relativePath = new File(path.relativePath, LsdfFilesService.getPath(first, more).path)
    }

    /**
     * Example: STORAGE_ROOT/dmg/otp/production/staging/...
     */
    File getAbsoluteStagingPath() {
        return getAbsolutePath(OperationType.DATA_PROCESSING, 'stagingRootPath')
    }

    /**
     * Example: ${otp.processing.root.path}/...
     */
    File getAbsoluteDataProcessingPath() {
        return getAbsolutePath(OperationType.DATA_PROCESSING, 'processingRootPath')
    }

    /**
     * Example: $OTP_ROOT_PATH/...
     */
    File getAbsoluteDataManagementPath() {
        return getAbsolutePath(OperationType.DATA_MANAGEMENT, 'rootPath')
    }

    private File getAbsolutePath(final OperationType operationType, final String rootPathName) {
        final Realm realm = ConfigService.getRealm(project, operationType)
        if (realm == null) {
            throw new RuntimeException("No ${operationType} realm found for project ${project}.")
        }
        final String rootPathString = realm."${rootPathName}"
        if (!rootPathString) {
            throw new RuntimeException("${rootPathName} is not set for ${realm}.")
        }
        final File rootPath = new File(rootPathString)
        if (!rootPath.isAbsolute()) {
            throw new RuntimeException("${rootPathName} (${rootPath}) is not absolute for ${realm}.")
        }
        return new File(rootPath, relativePath.path)
    }

    static final String PATH_COMPONENT_REGEX = /[a-zA-Z0-9_\-\+\.]+/
    static final String PATH_CHARACTERS_REGEX = /[a-zA-Z0-9_\-\+\.\/]+/
    static final Pattern PATH_COMPONENT_PATTERN = Pattern.compile(/^${PATH_COMPONENT_REGEX}$/)
    static final Pattern RELATIVE_PATH_PATTERN = Pattern.compile(/^${PATH_COMPONENT_REGEX}(?:\/${PATH_COMPONENT_REGEX})*$/)
    static final Pattern ABSOLUTE_PATH_PATTERN = Pattern.compile(/^(?:\/${PATH_COMPONENT_REGEX})+$/)
    static final Pattern ILLEGAL_IN_NORMALIZED_PATH = Pattern.compile(/(?:^|\/)\.{1,2}(?:\/|$)/)

    static boolean isValidPathComponent(String string) {
        return PATH_COMPONENT_PATTERN.matcher(string).matches() && !ILLEGAL_IN_NORMALIZED_PATH.matcher(string).find()
    }

    static boolean isValidRelativePath(String string) {
        return RELATIVE_PATH_PATTERN.matcher(string).matches() && !ILLEGAL_IN_NORMALIZED_PATH.matcher(string).find()
    }

    static boolean isValidAbsolutePath(String string) {
        return ABSOLUTE_PATH_PATTERN.matcher(string).matches() && !ILLEGAL_IN_NORMALIZED_PATH.matcher(string).find()
    }
}
