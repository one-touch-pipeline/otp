package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.job.processing.ProcessingException

@TestMixin(GrailsUnitTestMixin)
@TestFor(MetaDataService)
@Mock([MetaDataKey])
class MetaDataServiceUnitTests {

    MetaDataService metaDataService

    @Before
    public void setUp() throws Exception {
        metaDataService = new MetaDataService()
    }

    @After
    public void tearDown() throws Exception {
        metaDataService = null
    }

    void testAssertAllNecessaryKeysExist() {
        metaDataService.assertAllNecessaryKeysExist(createAllMetadataKeys())
    }

    void testAssertAllNecessaryKeysExistWithMissingKey() {
        [
            0..MetaDataColumn.values().length
        ].each {
            List<MetaDataKey> keys = createAllMetadataKeys()
            keys.remove(it)
            metaDataService.assertAllNecessaryKeysExist(keys)
        }
    }

    void testAssertAllNecessaryKeysExistWithOptionalKey() {
        List<MetaDataKey> keys = createAllMetadataKeys()
        keys.add(new MetaDataKey([name: "optional"]))
        metaDataService.assertAllNecessaryKeysExist(keys)
    }

    void testGetKeysFromTokens() {
        assertEquals(MetaDataColumn.values().length, metaDataService.getKeysFromTokens(MetaDataColumn.values()*.name()).size())
    }

    @Ignore
    void testGetKeysFromTokensWithMissingKey() {
        shouldFail(ProcessingException.class) {
            metaDataService.getKeysFromTokens(MetaDataColumn.values()*.name().subList(0, 10))
        }
    }

    void testGetKeysFromTokensWithOptionalKey() {
        List<String> keys = MetaDataColumn.values()*.name()
        keys.add("optional")
        assertEquals(MetaDataColumn.values().length + 1, metaDataService.getKeysFromTokens(keys).size())
    }

    private List<MetaDataKey> createAllMetadataKeys() {
        List<MetaDataKey> keys = []
        MetaDataColumn.values().each {
            keys << new MetaDataKey(
                            name: it.name()
                            )
        }
        return keys
    }
}
