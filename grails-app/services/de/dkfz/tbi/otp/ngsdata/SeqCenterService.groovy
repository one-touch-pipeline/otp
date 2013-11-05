
package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PostFilter;

class SeqCenterService {

    /**
     *
     * @return List of all available SeqCenters
     */
    @PostFilter("hasPermission(filterObject, 'read') or hasRole('ROLE_OPERATOR')")
    public List<SeqCenter> allSeqCenters() {
        return SeqCenter.list(sort: "name", order: "asc")
    }
}
