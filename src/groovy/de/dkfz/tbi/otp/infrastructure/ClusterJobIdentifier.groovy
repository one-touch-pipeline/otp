package de.dkfz.tbi.otp.infrastructure

import groovy.transform.EqualsAndHashCode

import de.dkfz.tbi.otp.ngsdata.Realm

@EqualsAndHashCode(includes = ["clusterJobId", "userName", "realmId"])
class ClusterJobIdentifier {

    final Realm realm

    final String clusterJobId

    final String userName

    def getRealmId() {
        realm.id
    }

    ClusterJobIdentifier(final Realm realm, final String clusterJobId, final String userName) {
        assert realm : "Realm not specified"
        assert clusterJobId : "Cluster job ID not specified"
        assert userName : "User name not specified"
        this.realm = realm
        this.clusterJobId = clusterJobId
        this.userName = userName
    }

    ClusterJobIdentifier(final ClusterJobIdentifier identifier) {
        this(identifier.realm, identifier.clusterJobId, identifier.userName)
    }

    ClusterJobIdentifier(final ClusterJob identifier) {
        this(identifier.realm, identifier.clusterJobId, identifier.userName)
    }


    @Override
    String toString() {
        return "Cluster job ${clusterJobId} on ${realm} with user ${userName}"
    }

    static List<ClusterJobIdentifier> asClusterJobIdentifierList(final Collection<? extends ClusterJob> c) {
        return c.collect( { new ClusterJobIdentifier(it) } )
    }
}
