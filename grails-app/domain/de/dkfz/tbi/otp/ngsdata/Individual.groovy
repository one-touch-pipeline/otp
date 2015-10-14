package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.Commentable
import de.dkfz.tbi.otp.dataprocessing.OtpPath

class Individual implements Commentable{

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
     * Holds an internal identifier, which shouldn't be visible for normal users.
     * Only admin users are allowed to see this value.
     */
    String internIdentifier

    Comment comment

    enum Type {REAL, POOL, CELLLINE, UNDEFINED}
    Type type

    Project project
    static belongsTo = [project : Project]




    static constraints = {
        pid(unique: true, nullable: false, validator: { OtpPath.isValidPathComponent(it) })
        internIdentifier(nullable: true)
        comment(nullable: true)
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
                seqPlatformModelLabel {
                    order("name")
                }
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

    /**
     * returns the folder viewByPid without the pid
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid
     */
    OtpPath getViewByPidPathBase(final SeqType seqType) {
        return new OtpPath(project, project.dirName, 'sequencing', seqType.dirName, 'view-by-pid')
    }

    /**
     * returns the folder viewByPid with the pid
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}
     */
    OtpPath getViewByPidPath(final SeqType seqType) {
        return new OtpPath(getViewByPidPathBase(seqType), pid)
    }

    /**
     * Example: ${project}/results_per_pid/${pid}
     */
    OtpPath getResultsPerPidPath() {
        return new OtpPath(project, project.dirName, 'results_per_pid', pid)
    }

    static mapping = {
        project index: "individual_project_idx"
    }
}
