package com.example.app;

public class TbmRecord {
    public final String id;
    public final String permitId;
    public final String date;
    public final String workName;
    public final String block;
    public final int participants;
    public final String briefing;
    public final boolean completed;

    public TbmRecord(String id, String permitId, String date, String workName,
                     String block, int participants, String briefing, boolean completed) {
        this.id = id;
        this.permitId = permitId;
        this.date = date;
        this.workName = workName;
        this.block = block;
        this.participants = participants;
        this.briefing = briefing;
        this.completed = completed;
    }
}
