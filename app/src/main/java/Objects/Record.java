package Objects;

public class Record {
    private double duration;
    private String UID;
    private String rid;
    private String rname;

    // Default constructor required for Firebase
    public Record() {}

    // Constructor
    public Record(double duration, String UID, String RID, String RName) {
        this.duration = duration;
        this.UID = UID;
        this.rid = RID;
        this.rname = RName;
    }

    // Getters and Setters
    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }

    public String getRname() {
        return rname;
    }

    public void setRname(String rname) {
        this.rname = rname;
    }
}
