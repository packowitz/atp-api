package io.pacworx.atp.notification;

public enum NotificationType {
    ANSWERABLE("1001", "answer", "ATP available", "Someone just started an ATP you could answer");

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
