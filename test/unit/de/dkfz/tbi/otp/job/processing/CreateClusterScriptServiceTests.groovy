package de.dkfz.tbi.otp.job.processing

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import static de.dkfz.tbi.otp.job.processing.CreateClusterScriptService.*


@TestFor(CreateClusterScriptService)
@Mock([Realm])
class CreateClusterScriptServiceTests {


    String SOURCE_BASE_1
    String SOURCE_DIR_1
    String SOURCE_FILE_1
    String SOURCE_FILE_2
    String SOURCE_BASE_2
    String SOURCE_DIR_2
    String SOURCE_FILE_3

    String TARGET_BASE_1
    String TARGET_DIR_1
    String TARGET_FILE_1
    String TARGET_FILE_2
    String TARGET_BASE_2
    String TARGET_DIR_2
    String TARGET_FILE_3

    String LINK_BASE_1
    String LINK_DIR_1
    String LINK_FILE_1
    String LINK_FILE_2
    String LINK_BASE_2
    String LINK_DIR_2
    String LINK_FILE_3

    List<String> fileNames
    List<String> dirNames

    List<File> sourceLocations
    List<File> targetLocations
    List<File> linkLocations
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

        fileNames = new LinkedList<String>()
        dirNames = new LinkedList<String>()

        SOURCE_BASE_1 = "${DKFZ_BASE}source"
        dirNames.add(SOURCE_BASE_1)
        SOURCE_DIR_1 = "${SOURCE_BASE_1}/dir1"
        dirNames.add(SOURCE_DIR_1)
        SOURCE_FILE_1 = "${SOURCE_DIR_1}/test1.txt"
        fileNames.add(SOURCE_FILE_1)
        SOURCE_FILE_2 = "${SOURCE_DIR_1}/test2.txt"
        fileNames.add(SOURCE_FILE_2)
        SOURCE_BASE_2 = "${BIOQUANT_BASE}source"
        dirNames.add(SOURCE_BASE_2)
        SOURCE_DIR_2 = "${SOURCE_BASE_2}/dir2"
        dirNames.add(SOURCE_DIR_2)
        SOURCE_FILE_3 = "${SOURCE_DIR_2}/test3.txt"
        fileNames.add(SOURCE_FILE_3)

        TARGET_BASE_1 = "${DKFZ_BASE}target"
        dirNames.add(TARGET_BASE_1)
        TARGET_DIR_1 = "${TARGET_BASE_1}/dir1"
        dirNames.add(TARGET_DIR_1)
        TARGET_FILE_1 = "${TARGET_DIR_1}/test1.txt"
        fileNames.add(TARGET_FILE_1)
        TARGET_FILE_2 = "${TARGET_DIR_1}/test2.txt"
        fileNames.add(TARGET_FILE_2)
        TARGET_BASE_2 = "${BIOQUANT_BASE}target"
        dirNames.add(TARGET_BASE_2)
        TARGET_DIR_2 = "${TARGET_BASE_2}/dir2"
        dirNames.add(TARGET_DIR_2)
        TARGET_FILE_3 = "${TARGET_DIR_2}/test3.txt"
        fileNames.add(TARGET_FILE_3)

        LINK_BASE_1 = "${DKFZ_BASE}link"
        dirNames.add(LINK_BASE_1)
        LINK_DIR_1 = "${LINK_BASE_1}/dir1"
        dirNames.add(LINK_DIR_1)
        LINK_FILE_1 = "${LINK_DIR_1}/test1.txt"
        fileNames.add(LINK_FILE_1)
        LINK_FILE_2 = "${LINK_DIR_1}/test2.txt"
        fileNames.add(LINK_FILE_2)
        LINK_BASE_2 = "${BIOQUANT_BASE}link"
        dirNames.add(LINK_BASE_2)
        LINK_DIR_2 = "${LINK_BASE_2}/dir2"
        dirNames.add(LINK_BASE_2)
        LINK_FILE_3 = "${LINK_DIR_2}/test3.txt"
        fileNames.add(LINK_DIR_2)

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

        Realm realm = DomainFactory.createRealmDataManagementBioQuant()
        realm.save(flush: true)
        hostname = "${realm.unixUser}@${realm.host}"
        port = realm.port
    }

    @After
    void tearDown() {
        sourceLocations = null
        targetLocations = null
        linkLocations = null
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
        sourceLocations.add(new File(SOURCE_FILE_1))
        targetLocations.add(new File(TARGET_FILE_1))
        linkLocations.add(new File(LINK_FILE_1))

        def expectedScript = """
set -e
if [ -f ${SOURCE_FILE_1} ]; then
md5sum ${SOURCE_FILE_1} > ${SOURCE_DIR_1}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_1};
cp  ${SOURCE_FILE_1} ${TARGET_FILE_1};
chmod 640 ${TARGET_FILE_1};
cp ${SOURCE_DIR_1}/${MD5SUM_NAME} ${TARGET_DIR_1}/${MD5SUM_NAME};
sed -i 's/test1.txt/test1.txt/' ${TARGET_DIR_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_1}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_DIR_1}; ln -s -f ${TARGET_FILE_1} ${LINK_FILE_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test(expected = IllegalArgumentException)
    void testCreateTransferScriptSourceLocationNull() {
        sourceLocations = null
        targetLocations.add(new File(TARGET_FILE_1))
        linkLocations.add(new File(LINK_FILE_1))
        service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
    }

    @Test(expected = IllegalArgumentException)
    void testCreateTransferScriptTargetLocationNull() {
        sourceLocations.add(new File(SOURCE_FILE_1))
        targetLocations = null
        linkLocations.add(new File(LINK_FILE_1))
        service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
    }

    @Test(expected = IllegalArgumentException)
    void testCreateTransferScriptLinkLocationNull() {
        sourceLocations.add(new File(SOURCE_FILE_1))
        targetLocations.add(new File(TARGET_FILE_1))
        linkLocations = null
        service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
    }

    @Test(expected = AssertionError)
    void testCreateTransferScriptListSizeDifferent() {
        sourceLocations.add(new File(SOURCE_FILE_1))
        sourceLocations.add(new File(SOURCE_FILE_2))
        targetLocations.add(new File(TARGET_FILE_1))
        linkLocations.add(new File(LINK_FILE_1))
        service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
    }

    @Test
    void testCreateTransferScriptCopyOneFileInEachList() {
        sourceLocations.add(new File(SOURCE_FILE_1))
        targetLocations.add(new File(TARGET_FILE_1))
        linkLocations.add(new File(LINK_FILE_1))
        def expectedScript = """
set -e
if [ -f ${SOURCE_FILE_1} ]; then
md5sum ${SOURCE_FILE_1} > ${SOURCE_DIR_1}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_1};
cp  ${SOURCE_FILE_1} ${TARGET_FILE_1};
chmod 640 ${TARGET_FILE_1};
cp ${SOURCE_DIR_1}/${MD5SUM_NAME} ${TARGET_DIR_1}/${MD5SUM_NAME};
sed -i 's/test1.txt/test1.txt/' ${TARGET_DIR_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_1}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_DIR_1}; ln -s -f ${TARGET_FILE_1} ${LINK_FILE_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyOneFileInListSourceNull() {
        sourceLocations.add(null)
        targetLocations.add(new File(TARGET_FILE_1))
        linkLocations.add(new File(LINK_FILE_1))
        def expectedScript = """
set -e
mkdir -p -m 2750 ${LINK_DIR_1}; ln -s -f ${TARGET_FILE_1} ${LINK_FILE_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyOneFileInListTargetNull() {
        sourceLocations.add(new File(SOURCE_FILE_1))
        targetLocations.add(null)
        linkLocations.add(new File(LINK_FILE_1))
        def expectedScript = """
set -e
mkdir -p -m 2750 ${LINK_DIR_1}; ln -s -f ${SOURCE_FILE_1} ${LINK_FILE_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyOneFileInListLinkNull() {
        sourceLocations.add(new File(SOURCE_FILE_1))
        targetLocations.add(new File(TARGET_FILE_1))
        linkLocations.add(null)
        def expectedScript = """
set -e
if [ -f ${SOURCE_FILE_1} ]; then
md5sum ${SOURCE_FILE_1} > ${SOURCE_DIR_1}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_1};
cp  ${SOURCE_FILE_1} ${TARGET_FILE_1};
chmod 640 ${TARGET_FILE_1};
cp ${SOURCE_DIR_1}/${MD5SUM_NAME} ${TARGET_DIR_1}/${MD5SUM_NAME};
sed -i 's/test1.txt/test1.txt/' ${TARGET_DIR_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_1}/${MD5SUM_NAME};
fi;
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyMultipleFilesInEachList() {
        sourceLocations.add(new File(SOURCE_FILE_1))
        sourceLocations.add(new File(SOURCE_FILE_2))
        targetLocations.add(new File(TARGET_FILE_1))
        targetLocations.add(new File(TARGET_FILE_2))
        linkLocations.add(new File(LINK_FILE_1))
        linkLocations.add(new File(LINK_FILE_2))
        def expectedScript = """
set -e
if [ -f ${SOURCE_FILE_1} ]; then
md5sum ${SOURCE_FILE_1} > ${SOURCE_DIR_1}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_1};
cp  ${SOURCE_FILE_1} ${TARGET_FILE_1};
chmod 640 ${TARGET_FILE_1};
cp ${SOURCE_DIR_1}/${MD5SUM_NAME} ${TARGET_DIR_1}/${MD5SUM_NAME};
sed -i 's/test1.txt/test1.txt/' ${TARGET_DIR_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_1}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_DIR_1}; ln -s -f ${TARGET_FILE_1} ${LINK_FILE_1};

if [ -f ${SOURCE_FILE_2} ]; then
md5sum ${SOURCE_FILE_2} > ${SOURCE_DIR_1}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_1};
cp  ${SOURCE_FILE_2} ${TARGET_FILE_2};
chmod 640 ${TARGET_FILE_2};
cp ${SOURCE_DIR_1}/${MD5SUM_NAME} ${TARGET_DIR_1}/${MD5SUM_NAME};
sed -i 's/test2.txt/test2.txt/' ${TARGET_DIR_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_1}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_DIR_1}; ln -s -f ${TARGET_FILE_2} ${LINK_FILE_2};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyMultipleFilesInListOneSourceNull() {
        sourceLocations.add(new File(SOURCE_FILE_1))
        sourceLocations.add(null)
        targetLocations.add(new File(TARGET_FILE_1))
        targetLocations.add(new File(TARGET_FILE_2))
        linkLocations.add(new File(LINK_FILE_1))
        linkLocations.add(new File(LINK_FILE_2))
        def expectedScript = """
set -e
if [ -f ${SOURCE_FILE_1} ]; then
md5sum ${SOURCE_FILE_1} > ${SOURCE_DIR_1}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_1};
cp  ${SOURCE_FILE_1} ${TARGET_FILE_1};
chmod 640 ${TARGET_FILE_1};
cp ${SOURCE_DIR_1}/${MD5SUM_NAME} ${TARGET_DIR_1}/${MD5SUM_NAME};
sed -i 's/test1.txt/test1.txt/' ${TARGET_DIR_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_1}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_DIR_1}; ln -s -f ${TARGET_FILE_1} ${LINK_FILE_1};

mkdir -p -m 2750 ${LINK_DIR_1}; ln -s -f ${TARGET_FILE_2} ${LINK_FILE_2};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptCopyMultipleFilesInListOneTargetNull() {
        sourceLocations.add(new File(SOURCE_FILE_1))
        sourceLocations.add(new File(SOURCE_FILE_2))
        targetLocations.add(new File(TARGET_FILE_1))
        targetLocations.add(null)
        linkLocations.add(new File(LINK_FILE_1))
        linkLocations.add(new File(LINK_FILE_2))
        def expectedScript = """
set -e
if [ -f ${SOURCE_FILE_1} ]; then
md5sum ${SOURCE_FILE_1} > ${SOURCE_DIR_1}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_1};
cp  ${SOURCE_FILE_1} ${TARGET_FILE_1};
chmod 640 ${TARGET_FILE_1};
cp ${SOURCE_DIR_1}/${MD5SUM_NAME} ${TARGET_DIR_1}/${MD5SUM_NAME};
sed -i 's/test1.txt/test1.txt/' ${TARGET_DIR_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_1}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_DIR_1}; ln -s -f ${TARGET_FILE_1} ${LINK_FILE_1};

mkdir -p -m 2750 ${LINK_DIR_1}; ln -s -f ${SOURCE_FILE_2} ${LINK_FILE_2};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyMultipleFilesInListOneLinkNull() {
        sourceLocations.add(new File(SOURCE_FILE_1))
        sourceLocations.add(new File(SOURCE_FILE_2))
        targetLocations.add(new File(TARGET_FILE_1))
        targetLocations.add(new File(TARGET_FILE_2))
        linkLocations.add(new File(LINK_FILE_1))
        linkLocations.add(null)
        def expectedScript = """
set -e
if [ -f ${SOURCE_FILE_1} ]; then
md5sum ${SOURCE_FILE_1} > ${SOURCE_DIR_1}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_1};
cp  ${SOURCE_FILE_1} ${TARGET_FILE_1};
chmod 640 ${TARGET_FILE_1};
cp ${SOURCE_DIR_1}/${MD5SUM_NAME} ${TARGET_DIR_1}/${MD5SUM_NAME};
sed -i 's/test1.txt/test1.txt/' ${TARGET_DIR_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_1}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_DIR_1}; ln -s -f ${TARGET_FILE_1} ${LINK_FILE_1};

if [ -f ${SOURCE_FILE_2} ]; then
md5sum ${SOURCE_FILE_2} > ${SOURCE_DIR_1}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_1};
cp  ${SOURCE_FILE_2} ${TARGET_FILE_2};
chmod 640 ${TARGET_FILE_2};
cp ${SOURCE_DIR_1}/${MD5SUM_NAME} ${TARGET_DIR_1}/${MD5SUM_NAME};
sed -i 's/test2.txt/test2.txt/' ${TARGET_DIR_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_1}/${MD5SUM_NAME};
fi;
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyOneDirectoryInEachList() {
        sourceLocations.add(new File(SOURCE_DIR_1))
        targetLocations.add(new File(TARGET_DIR_1))
        linkLocations.add(new File(LINK_DIR_1))
        def expectedScript = """
set -e
if [ -f ${SOURCE_DIR_1} ]; then
find ${SOURCE_DIR_1} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_1}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_BASE_1};
cp -r ${SOURCE_DIR_1} ${TARGET_DIR_1};
chmod 2750 ${TARGET_DIR_1};
cp ${SOURCE_BASE_1}/${MD5SUM_NAME} ${TARGET_BASE_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_BASE_1}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_1}/${MD5SUM_NAME};
rm -f ${SOURCE_BASE_1}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_BASE_1}; ln -s -f ${TARGET_DIR_1} ${LINK_DIR_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyOneDirectoryInListSourceNull() {
        sourceLocations.add(null)
        targetLocations.add(new File(TARGET_DIR_1))
        linkLocations.add(new File(LINK_DIR_1))
        def expectedScript = """
set -e
mkdir -p -m 2750 ${LINK_BASE_1}; ln -s -f ${TARGET_DIR_1} ${LINK_DIR_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyOneDirectoryInListTargetNull() {
        sourceLocations.add(new File(SOURCE_DIR_1))
        targetLocations.add(null)
        linkLocations.add(new File(LINK_DIR_1))
        def expectedScript = """
set -e
mkdir -p -m 2750 ${LINK_BASE_1}; ln -s -f ${SOURCE_DIR_1} ${LINK_DIR_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptCopyOneDirectoryInListLinkNull() {
        sourceLocations.add(new File(SOURCE_DIR_1))
        targetLocations.add(new File(TARGET_DIR_1))
        linkLocations.add(null)
        def expectedScript = """
set -e
if [ -f ${SOURCE_DIR_1} ]; then
find ${SOURCE_DIR_1} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_1}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_BASE_1};
cp -r ${SOURCE_DIR_1} ${TARGET_DIR_1};
chmod 2750 ${TARGET_DIR_1};
cp ${SOURCE_BASE_1}/${MD5SUM_NAME} ${TARGET_BASE_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_BASE_1}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_1}/${MD5SUM_NAME};
rm -f ${SOURCE_BASE_1}/${MD5SUM_NAME};
fi;
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptCopyFileAndDirectoryInEachList() {
        sourceLocations.add(new File(SOURCE_DIR_1))
        sourceLocations.add(new File(SOURCE_FILE_2))
        targetLocations.add(new File(TARGET_DIR_1))
        targetLocations.add(new File(TARGET_FILE_2))
        linkLocations.add(new File(LINK_DIR_1))
        linkLocations.add(new File(LINK_FILE_2))
        def expectedScript = """
set -e
if [ -f ${SOURCE_DIR_1} ]; then
find ${SOURCE_DIR_1} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_1}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_BASE_1};
cp -r ${SOURCE_DIR_1} ${TARGET_DIR_1};
chmod 2750 ${TARGET_DIR_1};
cp ${SOURCE_BASE_1}/${MD5SUM_NAME} ${TARGET_BASE_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_BASE_1}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_1}/${MD5SUM_NAME};
rm -f ${SOURCE_BASE_1}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_BASE_1}; ln -s -f ${TARGET_DIR_1} ${LINK_DIR_1};

if [ -f ${SOURCE_FILE_2} ]; then
md5sum ${SOURCE_FILE_2} > ${SOURCE_DIR_1}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_1};
cp  ${SOURCE_FILE_2} ${TARGET_FILE_2};
chmod 640 ${TARGET_FILE_2};
cp ${SOURCE_DIR_1}/${MD5SUM_NAME} ${TARGET_DIR_1}/${MD5SUM_NAME};
sed -i 's/test2.txt/test2.txt/' ${TARGET_DIR_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${SOURCE_DIR_1}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_DIR_1}; ln -s -f ${TARGET_FILE_2} ${LINK_FILE_2};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptMoveOneFileInEachList() {
        sourceLocations.add(new File(SOURCE_FILE_1))
        targetLocations.add(new File(TARGET_FILE_1))
        linkLocations.add(new File(LINK_FILE_1))
        move = true
        def expectedScript = """
set -e
if [ -f ${SOURCE_FILE_1} ]; then
md5sum ${SOURCE_FILE_1} > ${SOURCE_DIR_1}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_DIR_1};
cp  ${SOURCE_FILE_1} ${TARGET_FILE_1};
chmod 640 ${TARGET_FILE_1};
cp ${SOURCE_DIR_1}/${MD5SUM_NAME} ${TARGET_DIR_1}/${MD5SUM_NAME};
sed -i 's/test1.txt/test1.txt/' ${TARGET_DIR_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_1}/${MD5SUM_NAME};
rm  -f ${SOURCE_FILE_1};
rm -f ${SOURCE_DIR_1}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_DIR_1}; ln -s -f ${TARGET_FILE_1} ${LINK_FILE_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptMoveOneDirectoryInEachList() {
        sourceLocations.add(new File(SOURCE_DIR_1))
        targetLocations.add(new File(TARGET_DIR_1))
        linkLocations.add(new File(LINK_DIR_1))
        move = true

        def expectedScript = """
set -e
if [ -f ${SOURCE_DIR_1} ]; then
find ${SOURCE_DIR_1} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_1}/${MD5SUM_NAME};
mkdir -p -m 2750 ${TARGET_BASE_1};
cp -r ${SOURCE_DIR_1} ${TARGET_DIR_1};
chmod 2750 ${TARGET_DIR_1};
cp ${SOURCE_BASE_1}/${MD5SUM_NAME} ${TARGET_BASE_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_BASE_1}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_1}/${MD5SUM_NAME};
rm -r -f ${SOURCE_DIR_1};
rm -f ${SOURCE_BASE_1}/${MD5SUM_NAME};
fi;
mkdir -p -m 2750 ${LINK_BASE_1}; ln -s -f ${TARGET_DIR_1} ${LINK_DIR_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptMoveOneFileInEachListDKFZtoBQ() {
        sourceLocations.add(new File(SOURCE_FILE_1))
        targetLocations.add(new File(TARGET_FILE_3))
        linkLocations.add(new File(LINK_FILE_3))
        move = true
        def expectedScript = """
set -e
if [ -f ${SOURCE_FILE_1} ]; then
md5sum ${SOURCE_FILE_1} > ${SOURCE_DIR_1}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${TARGET_DIR_2}\";
scp -p ${port}  ${SOURCE_FILE_1} ${hostname}:${TARGET_FILE_3};
ssh -p ${port} ${hostname} \"chmod 640 ${TARGET_FILE_3}\";
scp -p ${port}  ${SOURCE_DIR_1}/${MD5SUM_NAME} ${hostname}:${TARGET_DIR_2}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"sed -i 's/test1.txt/test3.txt/' ${TARGET_DIR_2}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"md5sum -c ${TARGET_DIR_2}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_2}/${MD5SUM_NAME}\";
rm  -f ${SOURCE_FILE_1};
rm -f ${SOURCE_DIR_1}/${MD5SUM_NAME};
fi;
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${LINK_DIR_2}; ln -s -f ${TARGET_FILE_3} ${LINK_FILE_3}\";
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptMoveOneDirectoryInEachListDKFZtoBQ() {
        sourceLocations.add(new File(SOURCE_DIR_1))
        targetLocations.add(new File(TARGET_DIR_2))
        linkLocations.add(new File(LINK_DIR_2))
        move = true
        def expectedScript = """
set -e
if [ -f ${SOURCE_DIR_1} ]; then
find ${SOURCE_DIR_1} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_1}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${TARGET_BASE_2}\";
scp -p ${port} -r ${SOURCE_DIR_1} ${hostname}:${TARGET_DIR_2};
ssh -p ${port} ${hostname} \"chmod 2750 ${TARGET_DIR_2}\";
scp -p ${port} -r ${SOURCE_BASE_1}/${MD5SUM_NAME} ${hostname}:${TARGET_BASE_2}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"md5sum -c ${TARGET_BASE_2}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_2}/${MD5SUM_NAME}\";
rm -r -f ${SOURCE_DIR_1};
rm -f ${SOURCE_BASE_1}/${MD5SUM_NAME};
fi;
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${LINK_BASE_2}; ln -s -f ${TARGET_DIR_2} ${LINK_DIR_2}\";
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyOneDirectoryInEachListDKFZtoBQ() {
        sourceLocations.add(new File(SOURCE_DIR_1))
        targetLocations.add(new File(TARGET_DIR_2))
        linkLocations.add(new File(LINK_DIR_2))
        def expectedScript = """
set -e
if [ -f ${SOURCE_DIR_1} ]; then
find ${SOURCE_DIR_1} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_1}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${TARGET_BASE_2}\";
scp -p ${port} -r ${SOURCE_DIR_1} ${hostname}:${TARGET_DIR_2};
ssh -p ${port} ${hostname} \"chmod 2750 ${TARGET_DIR_2}\";
scp -p ${port} -r ${SOURCE_BASE_1}/${MD5SUM_NAME} ${hostname}:${TARGET_BASE_2}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"md5sum -c ${TARGET_BASE_2}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_2}/${MD5SUM_NAME}\";
rm -f ${SOURCE_BASE_1}/${MD5SUM_NAME};
fi;
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${LINK_BASE_2}; ln -s -f ${TARGET_DIR_2} ${LINK_DIR_2}\";
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyOneFileInEachListDKFZtoBQ() {
        sourceLocations.add(new File(SOURCE_FILE_1))
        targetLocations.add(new File(TARGET_FILE_3))
        linkLocations.add(new File(LINK_FILE_3))
        def expectedScript = """
set -e
if [ -f ${SOURCE_FILE_1} ]; then
md5sum ${SOURCE_FILE_1} > ${SOURCE_DIR_1}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${TARGET_DIR_2}\";
scp -p ${port}  ${SOURCE_FILE_1} ${hostname}:${TARGET_FILE_3};
ssh -p ${port} ${hostname} \"chmod 640 ${TARGET_FILE_3}\";
scp -p ${port}  ${SOURCE_DIR_1}/${MD5SUM_NAME} ${hostname}:${TARGET_DIR_2}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"sed -i 's/test1.txt/test3.txt/' ${TARGET_DIR_2}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"md5sum -c ${TARGET_DIR_2}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_2}/${MD5SUM_NAME}\";
rm -f ${SOURCE_DIR_1}/${MD5SUM_NAME};
fi;
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${LINK_DIR_2}; ln -s -f ${TARGET_FILE_3} ${LINK_FILE_3}\";
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptMoveOneDirectoryInEachListBQtoDKFZ() {
        sourceLocations.add(new File(SOURCE_DIR_2))
        targetLocations.add(new File(TARGET_DIR_1))
        linkLocations.add(new File(LINK_DIR_1))
        move = true
        def expectedScript = """
set -e
if [ -f ${SOURCE_DIR_2} ]; then
ssh -p ${port} ${hostname} \"find ${SOURCE_DIR_2} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_2}/${MD5SUM_NAME}\";
mkdir -p -m 2750 ${TARGET_BASE_1};
cp -r ${SOURCE_DIR_2} ${TARGET_DIR_1};
chmod 2750 ${TARGET_DIR_1};
cp ${SOURCE_BASE_2}/${MD5SUM_NAME} ${TARGET_BASE_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_BASE_1}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_1}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"rm -r -f ${SOURCE_DIR_2}\";
ssh -p ${port} ${hostname} \"rm -f ${SOURCE_BASE_2}/${MD5SUM_NAME}\";
fi;
mkdir -p -m 2750 ${LINK_BASE_1}; ln -s -f ${TARGET_DIR_1} ${LINK_DIR_1};
"""
        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptMoveOneFileInEachListBQtoDKFZ() {
        sourceLocations.add(new File(SOURCE_FILE_3))
        targetLocations.add(new File(TARGET_FILE_1))
        linkLocations.add(new File(LINK_FILE_1))
        move = true
        def expectedScript = """
set -e
if [ -f ${SOURCE_FILE_3} ]; then
ssh -p ${port} ${hostname} \"md5sum ${SOURCE_FILE_3} > ${SOURCE_DIR_2}/${MD5SUM_NAME}\";
mkdir -p -m 2750 ${TARGET_DIR_1};
cp  ${SOURCE_FILE_3} ${TARGET_FILE_1};
chmod 640 ${TARGET_FILE_1};
cp ${SOURCE_DIR_2}/${MD5SUM_NAME} ${TARGET_DIR_1}/${MD5SUM_NAME};
sed -i 's/test3.txt/test1.txt/' ${TARGET_DIR_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_1}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"rm  -f ${SOURCE_FILE_3}\";
ssh -p ${port} ${hostname} \"rm -f ${SOURCE_DIR_2}/${MD5SUM_NAME}\";
fi;
mkdir -p -m 2750 ${LINK_DIR_1}; ln -s -f ${TARGET_FILE_1} ${LINK_FILE_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }


    @Test
    void testCreateTransferScriptCopyOneDirectoryInEachListBQtoDKFZ() {
        sourceLocations.add(new File(SOURCE_DIR_2))
        targetLocations.add(new File(TARGET_DIR_1))
        linkLocations.add(new File(LINK_DIR_1))
        def expectedScript = """
set -e
if [ -f ${SOURCE_DIR_2} ]; then
ssh -p ${port} ${hostname} \"find ${SOURCE_DIR_2} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_2}/${MD5SUM_NAME}\";
mkdir -p -m 2750 ${TARGET_BASE_1};
cp -r ${SOURCE_DIR_2} ${TARGET_DIR_1};
chmod 2750 ${TARGET_DIR_1};
cp ${SOURCE_BASE_2}/${MD5SUM_NAME} ${TARGET_BASE_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_BASE_1}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_1}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"rm -f ${SOURCE_BASE_2}/${MD5SUM_NAME}\";
fi;
mkdir -p -m 2750 ${LINK_BASE_1}; ln -s -f ${TARGET_DIR_1} ${LINK_DIR_1};
"""
        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptCopyOneFileInEachListBQtoDKFZ() {
        sourceLocations.add(new File(SOURCE_FILE_3))
        targetLocations.add(new File(TARGET_FILE_1))
        linkLocations.add(new File(LINK_FILE_1))
        def expectedScript = """
set -e
if [ -f ${SOURCE_FILE_3} ]; then
ssh -p ${port} ${hostname} \"md5sum ${SOURCE_FILE_3} > ${SOURCE_DIR_2}/${MD5SUM_NAME}\";
mkdir -p -m 2750 ${TARGET_DIR_1};
cp  ${SOURCE_FILE_3} ${TARGET_FILE_1};
chmod 640 ${TARGET_FILE_1};
cp ${SOURCE_DIR_2}/${MD5SUM_NAME} ${TARGET_DIR_1}/${MD5SUM_NAME};
sed -i 's/test3.txt/test1.txt/' ${TARGET_DIR_1}/${MD5SUM_NAME};
md5sum -c ${TARGET_DIR_1}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_1}/${MD5SUM_NAME};
ssh -p ${port} ${hostname} \"rm -f ${SOURCE_DIR_2}/${MD5SUM_NAME}\";
fi;
mkdir -p -m 2750 ${LINK_DIR_1}; ln -s -f ${TARGET_FILE_1} ${LINK_FILE_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptMoveOneDirectoryInEachListAtBQ() {
        sourceLocations.add(new File(SOURCE_DIR_2))
        targetLocations.add(new File(TARGET_DIR_2))
        linkLocations.add(new File(LINK_DIR_2))
        move = true
        def expectedScript = """
set -e
if [ -f ${SOURCE_DIR_2} ]; then
ssh -p ${port} ${hostname} \"find ${SOURCE_DIR_2} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_2}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${TARGET_BASE_2}\";
ssh -p ${port} ${hostname} \"cp -r ${SOURCE_DIR_2} ${TARGET_DIR_2};
chmod 2750 ${TARGET_DIR_2};
cp ${SOURCE_BASE_2}/${MD5SUM_NAME} ${TARGET_BASE_2}/${MD5SUM_NAME}\";
ssh -p 22 unixUser2@otphost-other.example.org "md5sum -c ${TARGET_BASE_2}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_2}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"rm -r -f ${SOURCE_DIR_2}\";
ssh -p ${port} ${hostname} \"rm -f ${SOURCE_BASE_2}/${MD5SUM_NAME}\";
fi;
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${LINK_BASE_2}; ln -s -f ${TARGET_DIR_2} ${LINK_DIR_2}\";
"""
        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptMoveOneFileInEachListAtBQ() {
        sourceLocations.add(new File(SOURCE_FILE_3))
        targetLocations.add(new File(TARGET_FILE_3))
        linkLocations.add(new File(LINK_FILE_3))
        move = true
        def expectedScript = """
set -e
if [ -f ${SOURCE_FILE_3} ]; then
ssh -p ${port} ${hostname} \"md5sum ${SOURCE_FILE_3} > ${SOURCE_DIR_2}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${TARGET_DIR_2}\";
ssh -p ${port} ${hostname} \"cp  ${SOURCE_FILE_3} ${TARGET_FILE_3};
chmod 640 ${TARGET_FILE_3};
cp ${SOURCE_DIR_2}/${MD5SUM_NAME} ${TARGET_DIR_2}/${MD5SUM_NAME}";
ssh -p ${port} ${hostname} \"sed -i 's/test3.txt/test3.txt/' ${TARGET_DIR_2}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"md5sum -c ${TARGET_DIR_2}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_2}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"rm  -f ${SOURCE_FILE_3}\";
ssh -p ${port} ${hostname} \"rm -f ${SOURCE_DIR_2}/${MD5SUM_NAME}\";
fi;
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${LINK_DIR_2}; ln -s -f ${TARGET_FILE_3} ${LINK_FILE_3}\";
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptCopyOneDirectoryInEachListAtBQ() {
        sourceLocations.add(new File(SOURCE_DIR_2))
        targetLocations.add(new File(TARGET_DIR_2))
        linkLocations.add(new File(LINK_DIR_2))
        def expectedScript = """
set -e
if [ -f ${SOURCE_DIR_2} ]; then
ssh -p ${port} ${hostname} \"find ${SOURCE_DIR_2} -type f -exec md5sum '{}' \\; >> ${SOURCE_BASE_2}/${MD5SUM_NAME}";
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${TARGET_BASE_2}\";
ssh -p ${port} ${hostname} \"cp -r ${SOURCE_DIR_2} ${TARGET_DIR_2};
chmod 2750 ${TARGET_DIR_2};
cp ${SOURCE_BASE_2}/${MD5SUM_NAME} ${TARGET_BASE_2}/${MD5SUM_NAME}";
ssh -p ${port} ${hostname} "md5sum -c ${TARGET_BASE_2}/${MD5SUM_NAME};
rm -f ${TARGET_BASE_2}/${MD5SUM_NAME}";
ssh -p ${port} ${hostname} \"rm -f ${SOURCE_BASE_2}/${MD5SUM_NAME}\";
fi;
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${LINK_BASE_2}; ln -s -f ${TARGET_DIR_2} ${LINK_DIR_2}\";
"""
        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test
    void testCreateTransferScriptCopyOneFileInEachListAtBQ() {
        sourceLocations.add(new File(SOURCE_FILE_3))
        targetLocations.add(new File(TARGET_FILE_3))
        linkLocations.add(new File(LINK_FILE_3))
        def expectedScript = """
set -e
if [ -f ${SOURCE_FILE_3} ]; then
ssh -p ${port} ${hostname} \"md5sum ${SOURCE_FILE_3} > ${SOURCE_DIR_2}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${TARGET_DIR_2}\";
ssh -p ${port} ${hostname} \"cp  ${SOURCE_FILE_3} ${TARGET_FILE_3};
chmod 640 ${TARGET_FILE_3};
cp ${SOURCE_DIR_2}/${MD5SUM_NAME} ${TARGET_DIR_2}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"sed -i 's/test3.txt/test3.txt/' ${TARGET_DIR_2}/${MD5SUM_NAME}";
ssh -p ${port} ${hostname} \"md5sum -c ${TARGET_DIR_2}/${MD5SUM_NAME};
rm -f ${TARGET_DIR_2}/${MD5SUM_NAME}\";
ssh -p ${port} ${hostname} \"rm -f ${SOURCE_DIR_2}/${MD5SUM_NAME}\";
fi;
ssh -p ${port} ${hostname} \"mkdir -p -m 2750 ${LINK_DIR_2}; ln -s -f ${TARGET_FILE_3} ${LINK_FILE_3}\";
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }

    @Test(expected = RuntimeException)
    void testCreateTransferScriptLinkFromDKFZtoBQ() {
        sourceLocations.add(new File(SOURCE_FILE_1))
        targetLocations.add(null)
        linkLocations.add(new File(LINK_FILE_3))

        service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
    }

    @Test
    void testCreateTransferScriptLinkFromBQtoDKFZ() {
        sourceLocations.add(new File(SOURCE_FILE_3))
        targetLocations.add(null)
        linkLocations.add(new File(LINK_FILE_1))
        def expectedScript = """
set -e
mkdir -p -m 2750 ${LINK_DIR_1}; ln -s -f ${SOURCE_FILE_3} ${LINK_FILE_1};
"""

        String actualScript = service.createTransferScript(sourceLocations, targetLocations, linkLocations, move)
        assertEquals(expectedScript.trim(), actualScript.trim())
    }
}
