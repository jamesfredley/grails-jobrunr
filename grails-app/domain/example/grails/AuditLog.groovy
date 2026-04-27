package example.grails

class AuditLog {

    String jobId
    String jobName
    String oldState
    String newState
    Date dateCreated

    static constraints = {
        jobId blank: false, maxSize: 100
        jobName nullable: true, maxSize: 255
        oldState nullable: true, maxSize: 50
        newState blank: false, maxSize: 50
    }

    String toString() {
        "AuditLog[${jobId}]: ${oldState} -> ${newState}"
    }
}
