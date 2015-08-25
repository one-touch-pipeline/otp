package de.dkfz.tbi.otp.job.processing

import grails.test.mixin.*

import org.junit.*

import de.dkfz.tbi.otp.ngsdata.Realm
import static de.dkfz.tbi.otp.job.processing.CreateClusterScriptService.*

import de.dkfz.tbi.otp.WorkflowTestRealms


@TestFor(CreateClusterScriptService)
@Mock([Realm])
class CreateClusterScriptServiceTests {

    final String MD5SUM = "d8935c02eabda4e4cd04067e2890d5b9"

    String SOURCE_BASE_DKFZ
    String SOURCE_DIR_DKFZ
    String SOURCE_FILE_DKFZ_1
    String SOURCE_FILE_DKFZ_2
    String SOURCE_BASE_BQ
    String SOURCE_DIR_BQ
    String SOURCE_FILE_BQ

    String TARGET_BASE_DKFZ
    String TARGET_DIR_DKFZ
    String TARGET_FILE_DKFZ_1
    String TARGET_FILE_DKFZ_2
    String TARGET_BASE_BQ
    String TARGET_DIR_BQ
    String TARGET_FILE_BQ

    String LINK_BASE_DKFZ
    String LINK_DIR_DKFZ
    String LINK_FILE_DKFZ_1
    String LINK_FILE_DKFZ_2
    String LINK_BASE_BQ
    String LINK_DIR_BQ
    String LINK_FILE_BQ

    List<String> fileNames
    List<String> dirNames

    List<File> sourceLocations
    List<File> targetLocations
    List<File> linkLocations
    List<String> md5Sums
    boolean move
    String hostname
    int port

    @Before
    void setUp() {
        DKFZ_BASE = "/tmpSTORAGE_ROOT/"
        BIOQUANT_BASE = "/tmp$BQ_ROOTPATH/"

        sourceLocations = new LinkedList<File>()
        targetLocations = new LinkedList<File>()
        linkLocations = new LinkedList<File>()
        md5Sums = new LinkedList<String>()

        fileNames = new LinkedList<String>()
        dirNames = new LinkedList<String>()

        SOURCE_BASE_DKFZ = "${DKFZ_BASE}source"
        dirNames.add(SOURCE_BASE_DKFZ)
        SOURCE_DIR_DKFZ = "${SOURCE_BASE_DKFZ}/dir1"
        dirNames.add(SOURCE_DIR_DKFZ)
        SOURCE_FILE_DKFZ_1 = "${SOURCE_DIR_DKFZ}/test1.txt"
        fileNames.add(SOURCE_FILE_DKFZ_1)
        SOURCE_FILE_DKFZ_2 = "${SOURCE_DIR_DKFZ}/test2.txt"
        fileNames.add(SOURCE_FILE_DKFZ_2)
        SOURCE_BASE_BQ = "${BIOQUANT_BASE}source"
        dirNames.add(SOURCE_BASE_BQ)
        SOURCE_DIR_BQ = "${SOURCE_BASE_BQ}/dir2"
        dirNames.add(SOURCE_DIR_BQ)
        SOURCE_FILE_BQ = "${SOURCE_DIR_BQ}/test3.txt"
        fileNames.add(SOURCE_FILE_BQ)

        TARGET_BASE_DKFZ = "${DKFZ_BASE}target"
        dirNames.add(TARGET_BASE_DKFZ)
        TARGET_DIR_DKFZ = "${TARGET_BASE_DKFZ}/dir1"
        dirNames.add(TARGET_DIR_DKFZ)
        TARGET_FILE_DKFZ_1 = "${TARGET_DIR_DKFZ}/test1.txt"
        fileNames.add(TARGET_FILE_DKFZ_1)
        TARGET_FILE_DKFZ_2 = "${TARGET_DIR_DKFZ}/test2.txt"
        fileNames.add(TARGET_FILE_DKFZ_2)
        TARGET_BASE_BQ = "${BIOQUANT_BASE}target"
        dirNames.add(TARGET_BASE_BQ)
        TARGET_DIR_BQ = "${TARGET_BASE_BQ}/dir2"
        dirNames.add(TARGET_DIR_BQ)
        TARGET_FILE_BQ = "${TARGET_DIR_BQ}/test3.txt"
        fileNames.add(TARGET_FILE_BQ)

        LINK_BASE_DKFZ = "${DKFZ_BASE}link"
        dirNames.add(LINK_BASE_DKFZ)
        LINK_DIR_DKFZ = "${LINK_BASE_DKFZ}/dir1"
        dirNames.add(LINK_DIR_DKFZ)
        LINK_FILE_DKFZ_1 = "${LINK_DIR_DKFZ}/test1.txt"
        fileNames.add(LINK_FILE_DKFZ_1)
        LINK_FILE_DKFZ_2 = "${LINK_DIR_DKFZ}/test2.txt"
        fileNames.add(LINK_FILE_DKFZ_2)
        LINK_BASE_BQ = "${BIOQUANT_BASE}link"
        dirNames.add(LINK_BASE_BQ)
        LINK_DIR_BQ = "${LINK_BASE_BQ}/dir2"
        dirNames.add(LINK_BASE_BQ)
        LINK_FILE_BQ = "${LINK_DIR_BQ}/test3.txt"
        fileNames.add(LINK_DIR_BQ)

        dirNames.each{ String dirName ->
            File dir = new File(dirName)
            dir.mkdirs()
        }

        fileNames.each { String fileName ->
            File file = new File(fileName)
            file.createNewFile()
        }

        //False is the default value of the method, which is why I set it here explicitly
        move = false

        // TODO: OTP-1738: Remove hard-coded Realm names
        Realm realm = WorkflowTestRealms.createRealmDataManagementBioQuant()
        hostname = "${realm.unixUser}@${realm.host}"
        port = realm.port
    }

    @After
    void tearDown() {
        sourceLocations = null
        targetLocations = null
        linkLocations = null
        md5Sums = null
        hostname = null

        fileNames.each { String fileName ->
            new File(fileName).delete()
        }
        dirNames.each { String dirName ->
            new File(dirName).deleteDir()
        }
    }


    @Test
    void testCreateTransferScriptMoveParameterNull() {
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_1))
        targetLocations.add(new File(TARGET_FILE_DKFZ_1))
        linkLocations.add(new File(LINK_FILE_DKFZ_1))

        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_DKFZ_1} ]; then
md5sum ${SOURCE_FILE_DKFZ_1} > ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_DKFZ};
cp  ${SOURCE_FILE_DKFZ_1} ${TARGET_FILE_DKFZ_1};
chmod 640 ${TARGET_FILE_DKFZ_1};
cp ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME} ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
sed -i 's#${SOURCE_FILE_DKFZ_1}#${TARGET_FILE_DKFZ_1}#' ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_DIR_DKFZ}; ln -s -f ${TARGET_FILE_DKFZ_1} ${LINK_FILE_DKFZ_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test(expected = IllegalArgumentException)
    void testCreateTransferScriptSourceLocationNull() {
        sourceLocations = null
        targetLocations.add(new File(TARGET_FILE_DKFZ_1))
        linkLocations.add(new File(LINK_FILE_DKFZ_1))
        service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
    }

    @Test(expected = IllegalArgumentException)
    void testCreateTransferScriptTargetLocationNull() {
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_1))
        targetLocations = null
        linkLocations.add(new File(LINK_FILE_DKFZ_1))
        service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
    }

    @Test(expected = IllegalArgumentException)
    void testCreateTransferScriptLinkLocationNull() {
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_1))
        targetLocations.add(new File(TARGET_FILE_DKFZ_1))
        linkLocations = null
        service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
    }

    @Test(expected = AssertionError)
    void testCreateTransferScriptListSizeDifferent() {
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_1))
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_2))
        targetLocations.add(new File(TARGET_FILE_DKFZ_1))
        linkLocations.add(new File(LINK_FILE_DKFZ_1))
        service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
    }

    @Test
    void testCreateTransferScriptCopyOneFileInEachList() {
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_1))
        targetLocations.add(new File(TARGET_FILE_DKFZ_1))
        linkLocations.add(new File(LINK_FILE_DKFZ_1))
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_DKFZ_1} ]; then
md5sum ${SOURCE_FILE_DKFZ_1} > ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_DKFZ};
cp  ${SOURCE_FILE_DKFZ_1} ${TARGET_FILE_DKFZ_1};
chmod 640 ${TARGET_FILE_DKFZ_1};
cp ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME} ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
sed -i 's#${SOURCE_FILE_DKFZ_1}#${TARGET_FILE_DKFZ_1}#' ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_DIR_DKFZ}; ln -s -f ${TARGET_FILE_DKFZ_1} ${LINK_FILE_DKFZ_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyOneFileInListSourceNull() {
        sourceLocations.add(null)
        targetLocations.add(new File(TARGET_FILE_DKFZ_1))
        linkLocations.add(new File(LINK_FILE_DKFZ_1))
        def expectedScript = """
set -e
umask 027
mkdir -p -m 2750 ${LINK_DIR_DKFZ}; ln -s -f ${TARGET_FILE_DKFZ_1} ${LINK_FILE_DKFZ_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyOneFileInListTargetNull() {
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_1))
        targetLocations.add(null)
        linkLocations.add(new File(LINK_FILE_DKFZ_1))
        def expectedScript = """
set -e
umask 027
mkdir -p -m 2750 ${LINK_DIR_DKFZ}; ln -s -f ${SOURCE_FILE_DKFZ_1} ${LINK_FILE_DKFZ_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyOneFileInListLinkNull() {
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_1))
        targetLocations.add(new File(TARGET_FILE_DKFZ_1))
        linkLocations.add(null)
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_DKFZ_1} ]; then
md5sum ${SOURCE_FILE_DKFZ_1} > ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_DKFZ};
cp  ${SOURCE_FILE_DKFZ_1} ${TARGET_FILE_DKFZ_1};
chmod 640 ${TARGET_FILE_DKFZ_1};
cp ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME} ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
sed -i 's#${SOURCE_FILE_DKFZ_1}#${TARGET_FILE_DKFZ_1}#' ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
fi;
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyMultipleFilesInEachList() {
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_1))
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_2))
        targetLocations.add(new File(TARGET_FILE_DKFZ_1))
        targetLocations.add(new File(TARGET_FILE_DKFZ_2))
        linkLocations.add(new File(LINK_FILE_DKFZ_1))
        linkLocations.add(new File(LINK_FILE_DKFZ_2))
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_DKFZ_1} ]; then
md5sum ${SOURCE_FILE_DKFZ_1} > ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_DKFZ};
cp  ${SOURCE_FILE_DKFZ_1} ${TARGET_FILE_DKFZ_1};
chmod 640 ${TARGET_FILE_DKFZ_1};
cp ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME} ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
sed -i 's#${SOURCE_FILE_DKFZ_1}#${TARGET_FILE_DKFZ_1}#' ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_DIR_DKFZ}; ln -s -f ${TARGET_FILE_DKFZ_1} ${LINK_FILE_DKFZ_1};

if [ -f ${SOURCE_FILE_DKFZ_2} ]; then
md5sum ${SOURCE_FILE_DKFZ_2} > ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_DKFZ};
cp  ${SOURCE_FILE_DKFZ_2} ${TARGET_FILE_DKFZ_2};
chmod 640 ${TARGET_FILE_DKFZ_2};
cp ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME} ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
sed -i 's#${SOURCE_FILE_DKFZ_2}#${TARGET_FILE_DKFZ_2}#' ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_DIR_DKFZ}; ln -s -f ${TARGET_FILE_DKFZ_2} ${LINK_FILE_DKFZ_2};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyMultipleFilesInListOneSourceNull() {
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_1))
        sourceLocations.add(null)
        targetLocations.add(new File(TARGET_FILE_DKFZ_1))
        targetLocations.add(new File(TARGET_FILE_DKFZ_2))
        linkLocations.add(new File(LINK_FILE_DKFZ_1))
        linkLocations.add(new File(LINK_FILE_DKFZ_2))
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_DKFZ_1} ]; then
md5sum ${SOURCE_FILE_DKFZ_1} > ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_DKFZ};
cp  ${SOURCE_FILE_DKFZ_1} ${TARGET_FILE_DKFZ_1};
chmod 640 ${TARGET_FILE_DKFZ_1};
cp ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME} ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
sed -i 's#${SOURCE_FILE_DKFZ_1}#${TARGET_FILE_DKFZ_1}#' ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_DIR_DKFZ}; ln -s -f ${TARGET_FILE_DKFZ_1} ${LINK_FILE_DKFZ_1};

mkdir -p -m 2750 ${LINK_DIR_DKFZ}; ln -s -f ${TARGET_FILE_DKFZ_2} ${LINK_FILE_DKFZ_2};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptCopyMultipleFilesInListOneTargetNull() {
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_1))
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_2))
        targetLocations.add(new File(TARGET_FILE_DKFZ_1))
        targetLocations.add(null)
        linkLocations.add(new File(LINK_FILE_DKFZ_1))
        linkLocations.add(new File(LINK_FILE_DKFZ_2))
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_DKFZ_1} ]; then
md5sum ${SOURCE_FILE_DKFZ_1} > ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_DKFZ};
cp  ${SOURCE_FILE_DKFZ_1} ${TARGET_FILE_DKFZ_1};
chmod 640 ${TARGET_FILE_DKFZ_1};
cp ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME} ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
sed -i 's#${SOURCE_FILE_DKFZ_1}#${TARGET_FILE_DKFZ_1}#' ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_DIR_DKFZ}; ln -s -f ${TARGET_FILE_DKFZ_1} ${LINK_FILE_DKFZ_1};

mkdir -p -m 2750 ${LINK_DIR_DKFZ}; ln -s -f ${SOURCE_FILE_DKFZ_2} ${LINK_FILE_DKFZ_2};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyMultipleFilesInListOneLinkNull() {
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_1))
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_2))
        targetLocations.add(new File(TARGET_FILE_DKFZ_1))
        targetLocations.add(new File(TARGET_FILE_DKFZ_2))
        linkLocations.add(new File(LINK_FILE_DKFZ_1))
        linkLocations.add(null)
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_DKFZ_1} ]; then
md5sum ${SOURCE_FILE_DKFZ_1} > ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_DKFZ};
cp  ${SOURCE_FILE_DKFZ_1} ${TARGET_FILE_DKFZ_1};
chmod 640 ${TARGET_FILE_DKFZ_1};
cp ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME} ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
sed -i 's#${SOURCE_FILE_DKFZ_1}#${TARGET_FILE_DKFZ_1}#' ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_DIR_DKFZ}; ln -s -f ${TARGET_FILE_DKFZ_1} ${LINK_FILE_DKFZ_1};

if [ -f ${SOURCE_FILE_DKFZ_2} ]; then
md5sum ${SOURCE_FILE_DKFZ_2} > ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_DKFZ};
cp  ${SOURCE_FILE_DKFZ_2} ${TARGET_FILE_DKFZ_2};
chmod 640 ${TARGET_FILE_DKFZ_2};
cp ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME} ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
sed -i 's#${SOURCE_FILE_DKFZ_2}#${TARGET_FILE_DKFZ_2}#' ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
fi;
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyOneDirectoryInEachList() {
        sourceLocations.add(new File(SOURCE_DIR_DKFZ))
        targetLocations.add(new File(TARGET_DIR_DKFZ))
        linkLocations.add(new File(LINK_DIR_DKFZ))
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_DIR_DKFZ} ]; then
find ${SOURCE_DIR_DKFZ} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_DKFZ}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_BASE_DKFZ};
cp -r ${SOURCE_DIR_DKFZ} ${TARGET_DIR_DKFZ};
chmod 2750 ${TARGET_DIR_DKFZ};
cp ${SOURCE_BASE_DKFZ}/${MD5SUM_NAME} ${TARGET_BASE_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_BASE_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_DKFZ}/${MD5SUM_NAME};
rm -f ${SOURCE_BASE_DKFZ}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_BASE_DKFZ}; ln -s -f ${TARGET_DIR_DKFZ} ${LINK_DIR_DKFZ};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyOneDirectoryInListSourceNull() {
        sourceLocations.add(null)
        targetLocations.add(new File(TARGET_DIR_DKFZ))
        linkLocations.add(new File(LINK_DIR_DKFZ))
        def expectedScript = """
set -e
umask 027
mkdir -p -m 2750 ${LINK_BASE_DKFZ}; ln -s -f ${TARGET_DIR_DKFZ} ${LINK_DIR_DKFZ};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyOneDirectoryInListTargetNull() {
        sourceLocations.add(new File(SOURCE_DIR_DKFZ))
        targetLocations.add(null)
        linkLocations.add(new File(LINK_DIR_DKFZ))
        def expectedScript = """
set -e
umask 027
mkdir -p -m 2750 ${LINK_BASE_DKFZ}; ln -s -f ${SOURCE_DIR_DKFZ} ${LINK_DIR_DKFZ};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptCopyOneDirectoryInListLinkNull() {
        sourceLocations.add(new File(SOURCE_DIR_DKFZ))
        targetLocations.add(new File(TARGET_DIR_DKFZ))
        linkLocations.add(null)
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_DIR_DKFZ} ]; then
find ${SOURCE_DIR_DKFZ} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_DKFZ}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_BASE_DKFZ};
cp -r ${SOURCE_DIR_DKFZ} ${TARGET_DIR_DKFZ};
chmod 2750 ${TARGET_DIR_DKFZ};
cp ${SOURCE_BASE_DKFZ}/${MD5SUM_NAME} ${TARGET_BASE_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_BASE_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_DKFZ}/${MD5SUM_NAME};
rm -f ${SOURCE_BASE_DKFZ}/${MD5SUM_NAME};
fi;
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptCopyFileAndDirectoryInEachList() {
        sourceLocations.add(new File(SOURCE_DIR_DKFZ))
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_2))
        targetLocations.add(new File(TARGET_DIR_DKFZ))
        targetLocations.add(new File(TARGET_FILE_DKFZ_2))
        linkLocations.add(new File(LINK_DIR_DKFZ))
        linkLocations.add(new File(LINK_FILE_DKFZ_2))
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_DIR_DKFZ} ]; then
find ${SOURCE_DIR_DKFZ} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_DKFZ}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_BASE_DKFZ};
cp -r ${SOURCE_DIR_DKFZ} ${TARGET_DIR_DKFZ};
chmod 2750 ${TARGET_DIR_DKFZ};
cp ${SOURCE_BASE_DKFZ}/${MD5SUM_NAME} ${TARGET_BASE_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_BASE_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_DKFZ}/${MD5SUM_NAME};
rm -f ${SOURCE_BASE_DKFZ}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_BASE_DKFZ}; ln -s -f ${TARGET_DIR_DKFZ} ${LINK_DIR_DKFZ};

if [ -f ${SOURCE_FILE_DKFZ_2} ]; then
md5sum ${SOURCE_FILE_DKFZ_2} > ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_DKFZ};
cp  ${SOURCE_FILE_DKFZ_2} ${TARGET_FILE_DKFZ_2};
chmod 640 ${TARGET_FILE_DKFZ_2};
cp ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME} ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
sed -i 's#${SOURCE_FILE_DKFZ_2}#${TARGET_FILE_DKFZ_2}#' ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_DIR_DKFZ}; ln -s -f ${TARGET_FILE_DKFZ_2} ${LINK_FILE_DKFZ_2};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptMoveOneFileInEachList() {
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_1))
        targetLocations.add(new File(TARGET_FILE_DKFZ_1))
        linkLocations.add(new File(LINK_FILE_DKFZ_1))
        move = true
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_DKFZ_1} ]; then
md5sum ${SOURCE_FILE_DKFZ_1} > ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_DKFZ};
cp  ${SOURCE_FILE_DKFZ_1} ${TARGET_FILE_DKFZ_1};
chmod 640 ${TARGET_FILE_DKFZ_1};
cp ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME} ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
sed -i 's#${SOURCE_FILE_DKFZ_1}#${TARGET_FILE_DKFZ_1}#' ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm  -f ${SOURCE_FILE_DKFZ_1};
rm -f ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_DIR_DKFZ}; ln -s -f ${TARGET_FILE_DKFZ_1} ${LINK_FILE_DKFZ_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptMoveOneDirectoryInEachList() {
        sourceLocations.add(new File(SOURCE_DIR_DKFZ))
        targetLocations.add(new File(TARGET_DIR_DKFZ))
        linkLocations.add(new File(LINK_DIR_DKFZ))
        move = true

        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_DIR_DKFZ} ]; then
find ${SOURCE_DIR_DKFZ} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_DKFZ}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_BASE_DKFZ};
cp -r ${SOURCE_DIR_DKFZ} ${TARGET_DIR_DKFZ};
chmod 2750 ${TARGET_DIR_DKFZ};
cp ${SOURCE_BASE_DKFZ}/${MD5SUM_NAME} ${TARGET_BASE_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_BASE_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_DKFZ}/${MD5SUM_NAME};
rm -r -f ${SOURCE_DIR_DKFZ};
rm -f ${SOURCE_BASE_DKFZ}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_BASE_DKFZ}; ln -s -f ${TARGET_DIR_DKFZ} ${LINK_DIR_DKFZ};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptMoveOneFileInEachListDKFZtoBQ() {
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_1))
        targetLocations.add(new File(TARGET_FILE_BQ))
        linkLocations.add(new File(LINK_FILE_BQ))
        move = true
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_DKFZ_1} ]; then
md5sum ${SOURCE_FILE_DKFZ_1} > ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${TARGET_DIR_BQ}\";
scp -P ${port}  ${SOURCE_FILE_DKFZ_1} ${hostname}:${TARGET_FILE_BQ};
ssh -p ${port} ${hostname} \"chmod 640 ${TARGET_FILE_BQ}\";
scp -P ${port}  ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME} ${hostname}:${TARGET_DIR_BQ}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"sed -i 's#${SOURCE_FILE_DKFZ_1}#${TARGET_FILE_BQ}#' ${TARGET_DIR_BQ}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"md5sum -c ${TARGET_DIR_BQ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_BQ}/${MD5SUM_NAME}\";
rm  -f ${SOURCE_FILE_DKFZ_1};
rm -f ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
fi;
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${LINK_DIR_BQ}; ln -s -f ${TARGET_FILE_BQ} ${LINK_FILE_BQ}\";
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptMoveOneDirectoryInEachListDKFZtoBQ() {
        sourceLocations.add(new File(SOURCE_DIR_DKFZ))
        targetLocations.add(new File(TARGET_DIR_BQ))
        linkLocations.add(new File(LINK_DIR_BQ))
        move = true
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_DIR_DKFZ} ]; then
find ${SOURCE_DIR_DKFZ} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_DKFZ}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${TARGET_BASE_BQ}\";
scp -P ${port} -r ${SOURCE_DIR_DKFZ} ${hostname}:${TARGET_DIR_BQ};
ssh -p ${port} ${hostname} \"chmod 2750 ${TARGET_DIR_BQ}\";
scp -P ${port} -r ${SOURCE_BASE_DKFZ}/${MD5SUM_NAME} ${hostname}:${TARGET_BASE_BQ}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"md5sum -c ${TARGET_BASE_BQ}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_BQ}/${MD5SUM_NAME}\";
rm -r -f ${SOURCE_DIR_DKFZ};
rm -f ${SOURCE_BASE_DKFZ}/${MD5SUM_NAME};
fi;
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${LINK_BASE_BQ}; ln -s -f ${TARGET_DIR_BQ} ${LINK_DIR_BQ}\";
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyOneDirectoryInEachListDKFZtoBQ() {
        sourceLocations.add(new File(SOURCE_DIR_DKFZ))
        targetLocations.add(new File(TARGET_DIR_BQ))
        linkLocations.add(new File(LINK_DIR_BQ))
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_DIR_DKFZ} ]; then
find ${SOURCE_DIR_DKFZ} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_DKFZ}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${TARGET_BASE_BQ}\";
scp -P ${port} -r ${SOURCE_DIR_DKFZ} ${hostname}:${TARGET_DIR_BQ};
ssh -p ${port} ${hostname} \"chmod 2750 ${TARGET_DIR_BQ}\";
scp -P ${port} -r ${SOURCE_BASE_DKFZ}/${MD5SUM_NAME} ${hostname}:${TARGET_BASE_BQ}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"md5sum -c ${TARGET_BASE_BQ}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_BQ}/${MD5SUM_NAME}\";
rm -f ${SOURCE_BASE_DKFZ}/${MD5SUM_NAME};
fi;
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${LINK_BASE_BQ}; ln -s -f ${TARGET_DIR_BQ} ${LINK_DIR_BQ}\";
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyOneFileInEachListDKFZtoBQ() {
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_1))
        targetLocations.add(new File(TARGET_FILE_BQ))
        linkLocations.add(new File(LINK_FILE_BQ))
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_DKFZ_1} ]; then
md5sum ${SOURCE_FILE_DKFZ_1} > ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${TARGET_DIR_BQ}\";
scp -P ${port}  ${SOURCE_FILE_DKFZ_1} ${hostname}:${TARGET_FILE_BQ};
ssh -p ${port} ${hostname} \"chmod 640 ${TARGET_FILE_BQ}\";
scp -P ${port}  ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME} ${hostname}:${TARGET_DIR_BQ}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"sed -i 's#${SOURCE_FILE_DKFZ_1}#${TARGET_FILE_BQ}#' ${TARGET_DIR_BQ}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"md5sum -c ${TARGET_DIR_BQ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_BQ}/${MD5SUM_NAME}\";
rm -f ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
fi;
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${LINK_DIR_BQ}; ln -s -f ${TARGET_FILE_BQ} ${LINK_FILE_BQ}\";
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptMoveOneDirectoryInEachListBQtoDKFZ() {
        sourceLocations.add(new File(SOURCE_DIR_BQ))
        targetLocations.add(new File(TARGET_DIR_DKFZ))
        linkLocations.add(new File(LINK_DIR_DKFZ))
        move = true
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_DIR_BQ} ]; then
ssh -p ${port} ${hostname} \"find ${SOURCE_DIR_BQ} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_BQ}/${MD5SUM_NAME}\";
mkdir -p -m 2750 ${TARGET_BASE_DKFZ};
cp -r ${SOURCE_DIR_BQ} ${TARGET_DIR_DKFZ};
chmod 2750 ${TARGET_DIR_DKFZ};
cp ${SOURCE_BASE_BQ}/${MD5SUM_NAME} ${TARGET_BASE_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_BASE_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_DKFZ}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"rm -r -f ${SOURCE_DIR_BQ}\";
ssh -p ${port} ${hostname} \"rm -f ${SOURCE_BASE_BQ}/${MD5SUM_NAME}\";
fi;
mkdir -p -m 2750 ${LINK_BASE_DKFZ}; ln -s -f ${TARGET_DIR_DKFZ} ${LINK_DIR_DKFZ};
"""
        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptMoveOneFileInEachListBQtoDKFZ() {
        sourceLocations.add(new File(SOURCE_FILE_BQ))
        targetLocations.add(new File(TARGET_FILE_DKFZ_1))
        linkLocations.add(new File(LINK_FILE_DKFZ_1))
        move = true
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_BQ} ]; then
ssh -p ${port} ${hostname} \"md5sum ${SOURCE_FILE_BQ} > ${SOURCE_DIR_BQ}/${MD5SUM_NAME}\";
mkdir -p -m 2750 ${TARGET_DIR_DKFZ};
cp  ${SOURCE_FILE_BQ} ${TARGET_FILE_DKFZ_1};
chmod 640 ${TARGET_FILE_DKFZ_1};
cp ${SOURCE_DIR_BQ}/${MD5SUM_NAME} ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
sed -i 's#${SOURCE_FILE_BQ}#${TARGET_FILE_DKFZ_1}#' ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"rm  -f ${SOURCE_FILE_BQ}\";
ssh -p ${port} ${hostname} \"rm -f ${SOURCE_DIR_BQ}/${MD5SUM_NAME}\";
fi;
mkdir -p -m 2750 ${LINK_DIR_DKFZ}; ln -s -f ${TARGET_FILE_DKFZ_1} ${LINK_FILE_DKFZ_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyOneDirectoryInEachListBQtoDKFZ() {
        sourceLocations.add(new File(SOURCE_DIR_BQ))
        targetLocations.add(new File(TARGET_DIR_DKFZ))
        linkLocations.add(new File(LINK_DIR_DKFZ))
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_DIR_BQ} ]; then
ssh -p ${port} ${hostname} \"find ${SOURCE_DIR_BQ} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_BQ}/${MD5SUM_NAME}\";
mkdir -p -m 2750 ${TARGET_BASE_DKFZ};
cp -r ${SOURCE_DIR_BQ} ${TARGET_DIR_DKFZ};
chmod 2750 ${TARGET_DIR_DKFZ};
cp ${SOURCE_BASE_BQ}/${MD5SUM_NAME} ${TARGET_BASE_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_BASE_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_DKFZ}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"rm -f ${SOURCE_BASE_BQ}/${MD5SUM_NAME}\";
fi;
mkdir -p -m 2750 ${LINK_BASE_DKFZ}; ln -s -f ${TARGET_DIR_DKFZ} ${LINK_DIR_DKFZ};
"""
        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptCopyOneFileInEachListBQtoDKFZ() {
        sourceLocations.add(new File(SOURCE_FILE_BQ))
        targetLocations.add(new File(TARGET_FILE_DKFZ_1))
        linkLocations.add(new File(LINK_FILE_DKFZ_1))
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_BQ} ]; then
ssh -p ${port} ${hostname} \"md5sum ${SOURCE_FILE_BQ} > ${SOURCE_DIR_BQ}/${MD5SUM_NAME}\";
mkdir -p -m 2750 ${TARGET_DIR_DKFZ};
cp  ${SOURCE_FILE_BQ} ${TARGET_FILE_DKFZ_1};
chmod 640 ${TARGET_FILE_DKFZ_1};
cp ${SOURCE_DIR_BQ}/${MD5SUM_NAME} ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
sed -i 's#${SOURCE_FILE_BQ}#${TARGET_FILE_DKFZ_1}#' ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"rm -f ${SOURCE_DIR_BQ}/${MD5SUM_NAME}\";
fi;
mkdir -p -m 2750 ${LINK_DIR_DKFZ}; ln -s -f ${TARGET_FILE_DKFZ_1} ${LINK_FILE_DKFZ_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptMoveOneDirectoryInEachListAtBQ() {
        sourceLocations.add(new File(SOURCE_DIR_BQ))
        targetLocations.add(new File(TARGET_DIR_BQ))
        linkLocations.add(new File(LINK_DIR_BQ))
        move = true
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_DIR_BQ} ]; then
ssh -p ${port} ${hostname} \"find ${SOURCE_DIR_BQ} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_BQ}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${TARGET_BASE_BQ}\";
ssh -p ${port} ${hostname} \"cp -r ${SOURCE_DIR_BQ} ${TARGET_DIR_BQ};
chmod 2750 ${TARGET_DIR_BQ};
cp ${SOURCE_BASE_BQ}/${MD5SUM_NAME} ${TARGET_BASE_BQ}/${MD5SUM_NAME}\";
ssh -p 22 unixUser2@otphost-other.example.org "md5sum -c ${TARGET_BASE_BQ}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_BQ}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"rm -r -f ${SOURCE_DIR_BQ}\";
ssh -p ${port} ${hostname} \"rm -f ${SOURCE_BASE_BQ}/${MD5SUM_NAME}\";
fi;
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${LINK_BASE_BQ}; ln -s -f ${TARGET_DIR_BQ} ${LINK_DIR_BQ}\";
"""
        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptMoveOneFileInEachListAtBQ() {
        sourceLocations.add(new File(SOURCE_FILE_BQ))
        targetLocations.add(new File(TARGET_FILE_BQ))
        linkLocations.add(new File(LINK_FILE_BQ))
        move = true
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_BQ} ]; then
ssh -p ${port} ${hostname} \"md5sum ${SOURCE_FILE_BQ} > ${SOURCE_DIR_BQ}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${TARGET_DIR_BQ}\";
ssh -p ${port} ${hostname} \"cp  ${SOURCE_FILE_BQ} ${TARGET_FILE_BQ};
chmod 640 ${TARGET_FILE_BQ};
cp ${SOURCE_DIR_BQ}/${MD5SUM_NAME} ${TARGET_DIR_BQ}/${MD5SUM_NAME}";
ssh -p ${port} ${hostname} \"sed -i 's#${SOURCE_FILE_BQ}#${TARGET_FILE_BQ}#' ${TARGET_DIR_BQ}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"md5sum -c ${TARGET_DIR_BQ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_BQ}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"rm  -f ${SOURCE_FILE_BQ}\";
ssh -p ${port} ${hostname} \"rm -f ${SOURCE_DIR_BQ}/${MD5SUM_NAME}\";
fi;
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${LINK_DIR_BQ}; ln -s -f ${TARGET_FILE_BQ} ${LINK_FILE_BQ}\";
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptCopyOneDirectoryInEachListAtBQ() {
        sourceLocations.add(new File(SOURCE_DIR_BQ))
        targetLocations.add(new File(TARGET_DIR_BQ))
        linkLocations.add(new File(LINK_DIR_BQ))
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_DIR_BQ} ]; then
ssh -p ${port} ${hostname} \"find ${SOURCE_DIR_BQ} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_BQ}/${MD5SUM_NAME}";
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${TARGET_BASE_BQ}\";
ssh -p ${port} ${hostname} \"cp -r ${SOURCE_DIR_BQ} ${TARGET_DIR_BQ};
chmod 2750 ${TARGET_DIR_BQ};
cp ${SOURCE_BASE_BQ}/${MD5SUM_NAME} ${TARGET_BASE_BQ}/${MD5SUM_NAME}";
ssh -p ${port} ${hostname} "md5sum -c ${TARGET_BASE_BQ}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_BQ}/${MD5SUM_NAME}";
ssh -p ${port} ${hostname} \"rm -f ${SOURCE_BASE_BQ}/${MD5SUM_NAME}\";
fi;
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${LINK_BASE_BQ}; ln -s -f ${TARGET_DIR_BQ} ${LINK_DIR_BQ}\";
"""
        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptCopyOneFileInEachListAtBQ() {
        sourceLocations.add(new File(SOURCE_FILE_BQ))
        targetLocations.add(new File(TARGET_FILE_BQ))
        linkLocations.add(new File(LINK_FILE_BQ))
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_BQ} ]; then
ssh -p ${port} ${hostname} \"md5sum ${SOURCE_FILE_BQ} > ${SOURCE_DIR_BQ}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${TARGET_DIR_BQ}\";
ssh -p ${port} ${hostname} \"cp  ${SOURCE_FILE_BQ} ${TARGET_FILE_BQ};
chmod 640 ${TARGET_FILE_BQ};
cp ${SOURCE_DIR_BQ}/${MD5SUM_NAME} ${TARGET_DIR_BQ}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"sed -i 's#${SOURCE_FILE_BQ}#${TARGET_FILE_BQ}#' ${TARGET_DIR_BQ}/${MD5SUM_NAME}";
ssh -p ${port} ${hostname} \"md5sum -c ${TARGET_DIR_BQ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_BQ}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"rm -f ${SOURCE_DIR_BQ}/${MD5SUM_NAME}\";
fi;
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${LINK_DIR_BQ}; ln -s -f ${TARGET_FILE_BQ} ${LINK_FILE_BQ}\";
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test(expected = RuntimeException)
    void testCreateTransferScriptLinkFromDKFZtoBQ() {
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_1))
        targetLocations.add(null)
        linkLocations.add(new File(LINK_FILE_BQ))

        service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
    }

    @Test
    void testCreateTransferScriptLinkFromBQtoDKFZ() {
        sourceLocations.add(new File(SOURCE_FILE_BQ))
        targetLocations.add(null)
        linkLocations.add(new File(LINK_FILE_DKFZ_1))
        def expectedScript = """
set -e
umask 027
mkdir -p -m 2750 ${LINK_DIR_DKFZ}; ln -s -f ${SOURCE_FILE_BQ} ${LINK_FILE_DKFZ_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScript_TransferOneFileWithinDKFZ_md5SumExisting() {
            sourceLocations.add(new File(SOURCE_FILE_DKFZ_1))
            targetLocations.add(new File(TARGET_FILE_DKFZ_1))
            linkLocations.add(null)
            md5Sums.add(MD5SUM)
            def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_DKFZ_1} ]; then
echo '${MD5SUM}  ${TARGET_FILE_DKFZ_1}' > ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_DKFZ};
cp  ${SOURCE_FILE_DKFZ_1} ${TARGET_FILE_DKFZ_1};
chmod 640 ${TARGET_FILE_DKFZ_1};
cp ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME} ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
sed -i 's#${SOURCE_FILE_DKFZ_1}#${TARGET_FILE_DKFZ_1}#' ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
fi;
"""

            String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, md5Sums)
            assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScript_TransferOneFileWithinBQ_md5SumExisting() {
        sourceLocations.add(new File(SOURCE_FILE_BQ))
        targetLocations.add(new File(TARGET_FILE_BQ))
        linkLocations.add(null)
        md5Sums.add(MD5SUM)
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_BQ} ]; then
ssh -p ${port} ${hostname} \"echo '${MD5SUM}  ${TARGET_FILE_BQ}' > ${SOURCE_DIR_BQ}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${TARGET_DIR_BQ}\";
ssh -p ${port} ${hostname} \"cp  ${SOURCE_FILE_BQ} ${TARGET_FILE_BQ};
chmod 640 ${TARGET_FILE_BQ};
cp ${SOURCE_DIR_BQ}/${MD5SUM_NAME} ${TARGET_DIR_BQ}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"sed -i 's#${SOURCE_FILE_BQ}#${TARGET_FILE_BQ}#' ${TARGET_DIR_BQ}/${MD5SUM_NAME}";
ssh -p ${port} ${hostname} \"md5sum -c ${TARGET_DIR_BQ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_BQ}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"rm -f ${SOURCE_DIR_BQ}/${MD5SUM_NAME}\";
fi;
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, md5Sums)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScript_TransferOneFileFromDKFZtoBQ_md5SumExisting() {
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_1))
        targetLocations.add(new File(TARGET_FILE_BQ))
        linkLocations.add(null)
        md5Sums.add(MD5SUM)
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_DKFZ_1} ]; then
echo '${MD5SUM}  ${TARGET_FILE_BQ}' > ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${TARGET_DIR_BQ}\";
scp -P ${port}  ${SOURCE_FILE_DKFZ_1} ${hostname}:${TARGET_FILE_BQ};
ssh -p ${port} ${hostname} \"chmod 640 ${TARGET_FILE_BQ}\";
scp -P ${port}  ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME} ${hostname}:${TARGET_DIR_BQ}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"sed -i 's#${SOURCE_FILE_DKFZ_1}#${TARGET_FILE_BQ}#' ${TARGET_DIR_BQ}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"md5sum -c ${TARGET_DIR_BQ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_BQ}/${MD5SUM_NAME}\";
rm -f ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
fi;
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, md5Sums)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScript_TransferOneFileFromBQtoDKFZ_md5SumExisting() {
        sourceLocations.add(new File(SOURCE_FILE_BQ))
        targetLocations.add(new File(TARGET_FILE_DKFZ_1))
        linkLocations.add(null)
        md5Sums.add(MD5SUM)
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_BQ} ]; then
ssh -p ${port} ${hostname} \"echo '${MD5SUM}  ${TARGET_FILE_DKFZ_1}' > ${SOURCE_DIR_BQ}/${MD5SUM_NAME}\";
mkdir -p -m 2750 ${TARGET_DIR_DKFZ};
cp  ${SOURCE_FILE_BQ} ${TARGET_FILE_DKFZ_1};
chmod 640 ${TARGET_FILE_DKFZ_1};
cp ${SOURCE_DIR_BQ}/${MD5SUM_NAME} ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
sed -i 's#${SOURCE_FILE_BQ}#${TARGET_FILE_DKFZ_1}#' ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"rm -f ${SOURCE_DIR_BQ}/${MD5SUM_NAME}\";
fi;
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, md5Sums)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScript_TransferTwoFiles_md5SumExistingForOnlyOne(){
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_1))
        sourceLocations.add(new File(SOURCE_FILE_DKFZ_2))
        targetLocations.add(new File(TARGET_FILE_DKFZ_1))
        targetLocations.add(new File(TARGET_FILE_DKFZ_2))
        linkLocations.add(null)
        linkLocations.add(null)
        md5Sums.add(MD5SUM)
        md5Sums.add(null)
        def expectedScript = """
set -e
umask 027
if [ -f ${SOURCE_FILE_DKFZ_1} ]; then
echo '${MD5SUM}  ${TARGET_FILE_DKFZ_1}' > ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_DKFZ};
cp  ${SOURCE_FILE_DKFZ_1} ${TARGET_FILE_DKFZ_1};
chmod 640 ${TARGET_FILE_DKFZ_1};
cp ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME} ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
sed -i 's#${SOURCE_FILE_DKFZ_1}#${TARGET_FILE_DKFZ_1}#' ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
fi;
if [ -f ${SOURCE_FILE_DKFZ_2} ]; then
md5sum ${SOURCE_FILE_DKFZ_2} > ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_DKFZ};
cp  ${SOURCE_FILE_DKFZ_2} ${TARGET_FILE_DKFZ_2};
chmod 640 ${TARGET_FILE_DKFZ_2};
cp ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME} ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
sed -i 's#${SOURCE_FILE_DKFZ_2}#${TARGET_FILE_DKFZ_2}#' ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_DKFZ}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_DKFZ}/${MD5SUM_NAME};
fi;
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, md5Sums)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void test_makeDirs() {
        String result = service.makeDirs([new File("/asdf"), new File("/qwertz")], "777")
        assert result == 'umask 000; mkdir --parents --mode 777 /asdf /qwertz &>/dev/null; echo $?'

        result = service.makeDirs([new File("/asdf")], "750")
        assert result == 'umask 027; mkdir --parents --mode 750 /asdf &>/dev/null; echo $?'

        result = service.makeDirs([new File("/asdf")])
        assert result == ' mkdir --parents  /asdf &>/dev/null; echo $?'
    }
}
