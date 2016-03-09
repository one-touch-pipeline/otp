package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.ngsdata.Realm
import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = ["clusterJobId", "userName", "realmId"])
public class ClusterJobIdentifier {

    final Realm realm

    final String clusterJobId

    final String userName

    def getRealmId() {
        realm.id
    }

    public ClusterJobIdentifier(final Realm realm, final String clusterJobId, final String userName) {
        assert realm : "Realm not specified"
        assert clusterJobId : "Cluster job ID not specified"
        assert userName : "User name not specified"
        this.realm = realm
        this.clusterJobId = clusterJobId
        this.userName = userName
    }

    public ClusterJobIdentifier(final ClusterJobIdentifier identifier) {
        this(identifier.realm, identifier.clusterJobId, identifier.userName)
    }

    public ClusterJobIdentifier(final ClusterJob identifier) {
        this(identifier.realm, identifier.clusterJobId, identifier.userName)
    }


    @Override
    public String toString() {
        return "Cluster job ${clusterJobId} on ${realm} with user ${userName}"
    }

    public static List<ClusterJobIdentifier> asClusterJobIdentifierList(final Collection<? extends ClusterJob> c) {
        return c.collect( { new ClusterJobIdentifier(it) } )
    }
}
