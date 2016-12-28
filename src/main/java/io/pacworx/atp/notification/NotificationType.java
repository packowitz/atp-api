package io.pacworx.atp.notification;

public enum NotificationType {
    ANSWERABLE("1001", "answerable", "ATP available", "Someone just started an ATP you could answer"),
    ATP_FINISHED("1010", "atp-finished", "ATP finished", "Your ATP is completely answered"),
    ATP_ABUSED("1015", "atp-abused", "ATP canceled", "Users reported your ATP as inaceptable"),
    FEEDBACK_ANSWER("1020", "answer", "Feedback reply", "Your feedback got answered by the ATP Team"),
    ANNOUNCEMENT("1030", "announcement", "New Announcement", "Check out the new announcement for ATP");

    private String id;
    private String type;
    private String title;
    private String text;

    NotificationType(String id, String type, String title, String text) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }
}
