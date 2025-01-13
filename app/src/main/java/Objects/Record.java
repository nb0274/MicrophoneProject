package Objects;

public class Record {
    private double duration;
    private String UID;
    private String RID;
    private String RName;

    // Default constructor required for Firebase
    public Record() {}

    // Constructor
    public Record(double duration, String UID, String RID, String RName) {
        this.duration = duration;
        this.UID = UID;
        this.RID = RID;
        this.RName = RName;
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

    public String getRID() {
        return RID;
    }

    public void setRID(String RID) {
        this.RID = RID;
    }

    public String getRName() {
        return RName;
    }

    public void setRName(String RName) {
        this.RName = RName;
    }
}
