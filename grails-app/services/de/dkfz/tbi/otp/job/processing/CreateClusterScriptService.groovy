package de.dkfz.tbi.otp.job.processing

import static org.springframework.util.Assert.*
import grails.util.Environment
import de.dkfz.tbi.otp.ngsdata.Realm

/**
 * This Service provides helper-methods to create short cluster scripts.
 * These methods should be written in a generic way so that it is easy to reuse them.
 *
 */
class CreateClusterScriptService {

    /*
     * It is not possible to work with the realms here since we do not know if the processing root path
     * or the root path has to be used.
     * TODO: Fix when OTP-1067 is done
     */
    static String DKFZ_BASE = "STORAGE_ROOT/"
    static String BIOQUANT_BASE = "$BQ_ROOTPATH/"
    final static String MD5SUM_NAME = "md5sum.txt"
    final static String DIRECTORY_PERMISSION = "2750"
    final static String FILE_PERMISSION = "640"

    /**
     * Method to copy and/or link files.
     *
     * @param sourceLocations A list of files which shall be transferred
     * @param targetLocations A list containing the paths where the files should be transferred to
     * @param linkLocations A list containing the paths where the transferred file should be linked to
     * @param move Defines if the files should be copied or moved, per default the files are copied
     *
     * The source/target/link locations are provided in lists in a way that the first entry of each list
     * belongs to the first entries of the other lists.
     * When a file should not be linked after copying the entry in linkLocations must be null.
     * When a file should not be copied, but linked the entry in targetLocations must be null.
     * !The length of all three lists must be the same!
     *
     * @return A bash script which can be executed via OTP
     */
    String createTransferScript(List<File> sourceLocations, List<File> targetLocations, List<File> linkLocations, boolean move = false) {
        notNull(sourceLocations)
        notNull(targetLocations)
        notNull(linkLocations)
        assert(sourceLocations.size() == targetLocations.size())
        assert(sourceLocations.size() == linkLocations.size())

        final String CONNECT_TO_BQ = connectToBQCommand()
        StringBuilder copyScript = StringBuilder.newInstance()
        copyScript << "set -e\n"

        for (int i = 0; i < sourceLocations.size(); i ++) {
            File source = sourceLocations.get(i)
            File target = targetLocations.get(i)
            File link = linkLocations.get(i)

            String prefixDependingOnLocation
            String suffixDependingOnLocation

            // source and target are given -> file shall be copied
            if (source && target) {
                copyScript << createCopyScript(source, target, move)
            }
            // link files if needed
            if (link) {
                File fileToLink = target ?: source
                copyScript << createLinkScript(fileToLink, link)
            }
        }
        return copyScript
    }

    String createCopyScript(File source, File target, boolean move = false) {
        notNull(source)
        notNull(target)
        boolean sourceAtDKFZ
        boolean targetAtDKFZ
        String prefixDependingOnLocation
        String suffixDependingOnLocation
        final String CONNECT_TO_BQ = connectToBQCommand()
        StringBuilder copyScript = StringBuilder.newInstance()
        sourceAtDKFZ = source.getPath().startsWith(DKFZ_BASE)
        targetAtDKFZ = target.getPath().startsWith(DKFZ_BASE)
        final String ACCESS_PERMISSION = source.isDirectory() ? DIRECTORY_PERMISSION : FILE_PERMISSION
        final String COPY_PARAMETER = source.isDirectory() ? "-r" : ""

        // check that source file really exists in the file system
        assert source.exists()
        assert source.canRead()

        prefixDependingOnLocation = sourceAtDKFZ ? "" : "${CONNECT_TO_BQ} \""
        suffixDependingOnLocation = sourceAtDKFZ ? "" : "\""

        // it has to be checked in the bash script that the source is still there to prevent restart problems
        copyScript << "if [ -f ${source} ]; then\n"

        // create md5sum
        if (source.isDirectory()) {
            copyScript << "${prefixDependingOnLocation}find ${source.getPath()} -type f -exec md5sum '{}' \\; >> ${source.parent}/${MD5SUM_NAME}${suffixDependingOnLocation};\n"
        } else {
            copyScript << "${prefixDependingOnLocation}md5sum ${source} > ${source.parent}/${MD5SUM_NAME}${suffixDependingOnLocation};\n"
        }

        // create output directory
        prefixDependingOnLocation = targetAtDKFZ ? "" : "${CONNECT_TO_BQ} \""
        suffixDependingOnLocation = targetAtDKFZ ? "" : "\""
        copyScript << "${prefixDependingOnLocation}mkdir -p -m ${DIRECTORY_PERMISSION} ${target.parent}${suffixDependingOnLocation};\n"

        // transfer files ("target" will be overwritten if it exists already)
        if (sourceAtDKFZ && !targetAtDKFZ) {
            copyScript << "scp -p ${BQPort} ${COPY_PARAMETER} ${source} ${BQHostname}:${target};\n"
            copyScript << "${CONNECT_TO_BQ} \"chmod ${ACCESS_PERMISSION} ${target}\";\n"
            copyScript << "scp -p ${BQPort} ${COPY_PARAMETER} ${source.parent}/${MD5SUM_NAME} ${BQHostname}:${target.parent}/${MD5SUM_NAME};\n"
        } else {
            if (!sourceAtDKFZ && targetAtDKFZ) {
                prefixDependingOnLocation = ""
                suffixDependingOnLocation = ""
            } else {
                prefixDependingOnLocation = (sourceAtDKFZ && targetAtDKFZ) ? "" : "${CONNECT_TO_BQ} \""
                suffixDependingOnLocation = (sourceAtDKFZ && targetAtDKFZ) ? "" : "\""
            }
            copyScript << """\
${prefixDependingOnLocation}cp ${COPY_PARAMETER} ${source} ${target};
chmod ${ACCESS_PERMISSION} ${target};
cp ${source.parent}/${MD5SUM_NAME} ${target.parent}/${MD5SUM_NAME}${suffixDependingOnLocation};
"""
        }

        prefixDependingOnLocation = targetAtDKFZ ? "" : "${CONNECT_TO_BQ} \""
        suffixDependingOnLocation = targetAtDKFZ ? "" : "\""
        // if the name of the copied file changed between source and target, the new name has to be inserted in the md5sum-file
        if (source.isFile()) {
            copyScript << """\
${prefixDependingOnLocation}sed -i 's/${source.getName()}/${target.getName()}/' ${target.parent}/${MD5SUM_NAME}${suffixDependingOnLocation};
"""
        }

        // check md5sum & delete md5sum file afterwards
        copyScript << """\
${prefixDependingOnLocation}md5sum -c ${target.parent}/${MD5SUM_NAME};
rm -f ${target.parent}/${MD5SUM_NAME}${suffixDependingOnLocation};
"""

        // after the successful check of the md5sums delete the files at the source location
        prefixDependingOnLocation = sourceAtDKFZ ? "" : "${CONNECT_TO_BQ} \""
        suffixDependingOnLocation = sourceAtDKFZ ? "" : "\""
        if (move) {
            copyScript << "${prefixDependingOnLocation}rm ${COPY_PARAMETER} -f ${source}${suffixDependingOnLocation};\n"
        }
        copyScript << "${prefixDependingOnLocation}rm -f ${source.parent}/${MD5SUM_NAME}${suffixDependingOnLocation};\n"

        copyScript << "fi;\n"
        return copyScript
    }


    String createLinkScript(File source, File link) {
        notNull(source)
        notNull(link)
        boolean linkAtDKFZ

        String prefixDependingOnLocation
        String suffixDependingOnLocation
        final String CONNECT_TO_BQ = connectToBQCommand()
        StringBuilder copyScript = StringBuilder.newInstance()
        boolean fileToLinkAtDKFZ = source.getPath().startsWith(DKFZ_BASE)
        linkAtDKFZ = link.getPath().startsWith(DKFZ_BASE)

        if (fileToLinkAtDKFZ && !linkAtDKFZ) {
            // target is at BQ but link must be here -> does not work
            throw new RuntimeException("It is not possible to create a link for BQ to DKFZ for file ${source}")
        } else {
            /*
             * In case the file which has to be linked is located at the DKFZ lsdf it does not matter if it shall be
             * linked to the BQ or the DKFZ. Since the BQ is mounted read only to the DKFZ it is possible to link the file
             * the same way as it would be within the DKFZ ldsf.
             * In case the file to link and the link are both at the BQ the commands have to be run at the BQ cluster.
             */
            prefixDependingOnLocation = (!fileToLinkAtDKFZ && !linkAtDKFZ) ? "${CONNECT_TO_BQ} \"" : ""
            suffixDependingOnLocation = (!fileToLinkAtDKFZ && !linkAtDKFZ) ? "\"" : ""
            copyScript << "${prefixDependingOnLocation}mkdir -p -m ${DIRECTORY_PERMISSION} ${link.parent}; ln -s -f ${source} ${link}${suffixDependingOnLocation};\n\n"
        }
        return copyScript
    }

    String connectToBQCommand() {
        return "ssh -p ${BQPort} ${BQHostname}"
    }

    String getBQHostname() {
        return "${bioquantRealm.unixUser}@${bioquantRealm.host}"
    }

    String getBQPort() {
        return bioquantRealm.port
    }

    Realm getBioquantRealm() {
        return Realm.findByNameAndClusterAndEnv('BioQuant', Realm.Cluster.BIOQUANT, Environment.current.name)
    }
}
