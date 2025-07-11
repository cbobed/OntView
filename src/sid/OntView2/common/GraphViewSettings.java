package sid.OntView2.common;

public class GraphViewSettings {
    private static GraphViewSettings instance;
    private ViewMode lastMode = ViewMode.ALL;
    private GraphViewSettings() {}

    public static synchronized GraphViewSettings getInstance() {
        if (instance == null) {
            instance = new GraphViewSettings();
        }
        return instance;
    }

    public ViewMode getLastMode() {
        return lastMode;
    }

    public void setLastMode(ViewMode mode) {
        this.lastMode = mode;
    }

}