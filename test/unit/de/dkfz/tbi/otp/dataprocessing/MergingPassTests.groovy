package de.dkfz.tbi.otp.dataprocessing

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

@TestFor(MergingPass)
@Build([MergingPass])
class MergingPassTests {

    void testIsLatestPass() {
        MergingSet mergingSet = MergingSet.build()

        MergingPass pass = MergingPass.build(
                identifier: 1,
                mergingSet: mergingSet)
        assertTrue(pass.isLatestPass())

        MergingPass pass2 = MergingPass.build(
                identifier: 2,
                mergingSet: mergingSet)
        assertTrue(pass2.isLatestPass())

        assertFalse(pass.isLatestPass())
    }
}
