package de.dkfz.tbi.otp.ngsdata

class Individual {

    /**
     * Identifier used in the file system. It should never change.
     * Usually its the same as {@link #mockPid}
     */
    String pid                 // real pid from iChip

    /**
     * Identifier used for displaying in tables/graphs.
     */
    String mockPid             // pid used in the project

    /**
     * Alternative Identifier. If an individual is renamed, this column can contain the old identifier.
     * Also it can be used for visible mapping of identifier.
     */
    String mockFullName        // mnemonic used in the project

    /**
     * Holds an intern identifier, which shouldn't visible for normal users.
     * Only admin users are allowed to see this value.
     */
    String internIdentifier


    enum Type {REAL, POOL, CELLLINE, UNDEFINED}
    Type type

    static belongsTo = [project : Project]

    static constraints = {
        pid(unique: true, nullable: false)
        internIdentifier(nullable: true)
    }

    String toString() {
        "${mockPid}"
    }

    /**
     * @return List of Sample for this Individual
     **/
    List<Sample> getSamples() {
        return Sample.findAllByIndividual(this)
    }

    /**
     * @return List of SeqType for this Individual
     **/
    List<SeqType> getSeqTypes() {
        return SeqType.executeQuery(
        '''
SELECT DISTINCT type from SeqScan scan
INNER JOIN scan.sample AS sample
INNER JOIN scan.seqType as type
WHERE sample.individual = :ind
order by type.name asc, type.libraryLayout
        ''', [ind: this])
    }

    /**
     * @return List of SeqScan for this Individual ordered
     **/
    List<SeqScan> getSeqScans() {
        def c = SeqScan.createCriteria()
        return c.list {
            sample {
                individual {
                    eq("id", this.id)
                }
            }
            seqPlatform {
                order("name")
                order("model")
            }
            sample {
                sampleType {
                    order ("name")
                }
            }
            order ("state")
            order ("nLanes")
        }
    }

    /**
     * @return List of Mutation for this Individual
     **/
    List<Mutation> getMutations() {
        return Mutation.findAllByIndividual(this)
    }
}
