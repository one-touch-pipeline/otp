package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.OtpPath

import static org.springframework.util.Assert.*
import org.springframework.security.access.prepost.PreAuthorize

class AntibodyTargetService {

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public AntibodyTarget createAntibodyTarget(String name){
        assert name : "the input name '${name}' must not be null"
        assert !AntibodyTarget.findByNameIlike(name) : "The AntibodyTarget '${name}' exists already"
        AntibodyTarget antibodyTarget = new AntibodyTarget(
                name: name
        )
        assert antibodyTarget.save(flush: true, failOnError: true)
        return antibodyTarget
    }
}