package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.access.prepost.PostAuthorize

class IndividualService {

    // TODO: this constant should not be here! And is also wrong!
    private static final String resourcesPath = "${home}STS/ngsTest/resources/"

    /**
     * this function loads individuals name from the provided list
     */
    void loadIndividuals() {
        // PROJECT_NAME
        Project proj = Project.findByName("PROJECT_NAME")

        File file = new File(resourcesPath + "listInd.txt")
        if (!file.canRead()) {
            // TODO: shall be exception
            log.warn "can not read ${file}"
            return
        }

        log.debug "reading list of individuals from ${file}"

        Individual ind
        file.eachLine { line, no ->
            // magic standard to DB relation
            String pid = line.substring(0, line.indexOf(','))
            String name = line.substring(line.indexOf(',') + 1)
            String mockPid = (no as String).padLeft(3, '0')

            ind = new Individual(mockFullName: name, pid: pid, mockPid: mockPid)
            ind.type = Individual.Type.REAL

            // HACK
            if (name.indexOf("Pool") != -1) {
                ind.type = Individual.Type.POOL
            }

            ind.project = proj
            ind.save()
        }
    }

    /**
     * this function load samples from the provided list
     * and attache samples to individuals
     * @return
     */
    def loadSamples() {
        // PROJECT_NAME

        // TODO: we are not in this directory
        String prefix = "${home}"

        def files = [
            "$DATA_HOME/PROJECT_NAME/map-pid-tumor-control.csv",
            "$DATA_HOME/PROJECT_NAME/map-pid-tumor-control-with-prefix.csv"
        ]

        files.each {
            Sample sample
            File file = new File(prefix + it);
            if (!file.canRead()) {
                log.debug "can not read ${file}"
                // continue
                return
            }

            log.debug "reading samples from file ${it}"

            file.eachLine { line, no ->

                // magic file format
                String name = line.substring(0, line.indexOf(','));
                String pid  = line.substring(line.indexOf(',') + 1, line.indexOf('/'))

                // find out type
                String typeText  = line.substring(line.indexOf('/') + 1)
                Sample.Type type = Sample.Type.UNKNOWN
                if (typeText.indexOf("tumor") != -1) {
                    type = Sample.Type.TUMOR
                }
                if (typeText.indexOf("control") != -1) {
                    type = Sample.Type.CONTROL
                }

                Individual ind = Individual.findByPid(pid);

                sample = Sample.findByIndividualAndType(ind, type)

                if (!sample) {
                    sample = new Sample(type: type)
                    sample.individual = ind
                    sample.save()
                }

                SampleIdentifier identifier = new SampleIdentifier(
                    name: name,
                    sample: sample
                )

                identifier.save()
                ind.save()
                sample.save()
            }
        }
    }

    /**
     * Retrieves the given Individual.
     * If the parameter can be converted to a Long it is assumed to be the database ID.
     * Otherwise it is tried to find the Individual by its mock name - as well if the Individual could not
     * be found by the database id.
     *
     * If no Individual is found null is returned.
     * @param identifier Name or database Id
     * @return Individual
     **/
    @PostAuthorize("(returnObject == null) or hasPermission(returnObject.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read) or hasRole('ROLE_ADMIN')")
    Individual getIndividual(String identifier) {
        if (!identifier) {
            return null
        }
        Individual individual = null
        if (identifier?.isLong()) {
            individual = Individual.get(identifier as Long)
        }
        if (!individual) {
            individual = Individual.findByMockFullName(identifier)
        }
        return individual
    }

    /**
     * Retrieves the given Individual.
     * Overloaded method for convenience.
     * @param identifier Name or database Id
     * @return Individual
     **/
    @PostAuthorize("(returnObject == null) or hasPermission(returnObject.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read) or hasRole('ROLE_ADMIN')")
    Individual getIndividual(long identifier) {
        return getIndividual("${identifier}")
    }

    /**
     * Retrieves the previous Individual by database id if present.
     * @param individual The Individual for which the predecessor has to be retrieved
     * @return Previous Individual if present, otherwise null
     **/
    @PreAuthorize("hasPermission(#individual.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read) or hasRole('ROLE_ADMIN')")
    Individual previousIndividual(Individual individual) {
        if (!individual) {
            return null
        }
        // TODO: navigate to Individual in ACL aware manner
        return Individual.findByIdLessThan(individual.id, [sort: "id", order: "desc"])
    }

    /**
     * Retrieves the next Individual by database id if present.
     * @param individual The Individual for which the successor has to be retrieved
     * @return Next Individual if present, otherwise null
     **/
    @PreAuthorize("hasPermission(#individual.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read) or hasRole('ROLE_ADMIN')")
    Individual nextIndividual(Individual individual) {
        if (!individual) {
            return null
        }
        // TODO: navigate to Individual in ACL aware manner
        return Individual.findByIdGreaterThan(individual.id, [sort: "id", oder: "asc"])
    }
}
