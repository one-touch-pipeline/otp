package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.ngsdata.Realm
import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor

@EqualsAndHashCode
public class ClusterJobIdentifierImpl implements ClusterJobIdentifier {

    final Realm realm

    final String clusterJobId

    public ClusterJobIdentifierImpl(final Realm realm, final String clusterJobId) {
        this.realm = realm
        this.clusterJobId = clusterJobId
    }

    public ClusterJobIdentifierImpl(final ClusterJobIdentifier identifier) {
        this.realm = identifier.realm
        this.clusterJobId = identifier.clusterJobId
    }

    @Override
    public String toString() {
        return "Cluster job ${clusterJobId} on ${realm}"
    }

    public static List<ClusterJobIdentifierImpl> asClusterJobIdentifierImplList(final Collection<? extends ClusterJobIdentifier> c) {
        return c.collect( { new ClusterJobIdentifierImpl(it) } )
    }

    private static Set<ClusterJobIdentifierImpl> asClusterJobIdentifierImplSet(final Collection<? extends ClusterJobIdentifier> c) {
        return asClusterJobIdentifierImplList(c).toSet()
    }

    public static boolean containSame(final Collection<? extends ClusterJobIdentifier> c1, final Collection<? extends ClusterJobIdentifier> c2) {
        return asClusterJobIdentifierImplSet(c1) == asClusterJobIdentifierImplSet(c2)
    }
}
