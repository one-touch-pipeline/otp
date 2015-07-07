package de.dkfz.tbi.otp.infrastructure

import static de.dkfz.tbi.otp.utils.HelperUtils.*

import org.junit.Test

import de.dkfz.tbi.otp.ngsdata.Realm
import grails.buildtestdata.mixin.Build

@Build([
        ClusterJob,
])
class ClusterJobUnitTests {

    @Test
    void testFindByClusterJobIdentifier() {
        ClusterJob clusterJob = ClusterJob.build()
        ClusterJobIdentifier identifier = new ClusterJobIdentifierImpl(clusterJob)
        ClusterJob differentJobId = ClusterJob.build(
                realm: identifier.realm,
                clusterJobId: getUniqueString(),
        )
        ClusterJob differentRealm = ClusterJob.build(
                realm: Realm.build(),
                clusterJobId: identifier.clusterJobId,
        )

        assert clusterJob.findByClusterJobIdentifier(identifier) == clusterJob
    }
}
