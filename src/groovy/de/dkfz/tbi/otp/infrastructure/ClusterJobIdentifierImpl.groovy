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
}
