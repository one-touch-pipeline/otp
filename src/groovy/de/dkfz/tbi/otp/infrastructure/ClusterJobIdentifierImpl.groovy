package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.ngsdata.Realm
import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor

@TupleConstructor  // see http://jeffastorey.blogspot.de/2011/10/final-variables-in-groovy-with-dynamic.html
@EqualsAndHashCode
public class ClusterJobIdentifierImpl implements ClusterJobIdentifier {

    final Realm realm

    final String clusterJobId

    @Override
    public String toString() {
        return "Cluster job ${clusterJobId} on realm ${realm}"
    }

    private static Set<ClusterJobIdentifierImpl> asClusterJobIdentifierImplSet(final Collection<? extends ClusterJobIdentifier> c) {
        return c.collect( { new ClusterJobIdentifierImpl(it.realm, it.clusterJobId) } ).toSet()
    }

    public static boolean containSame(final Collection<? extends ClusterJobIdentifier> c1, final Collection<? extends ClusterJobIdentifier> c2) {
        return asClusterJobIdentifierImplSet(c1) == asClusterJobIdentifierImplSet(c2)
    }
}
