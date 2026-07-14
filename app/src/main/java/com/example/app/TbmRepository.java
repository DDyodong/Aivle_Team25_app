package com.example.app;

import java.util.List;

public interface TbmRepository {
    boolean login(String employeeNo, String password);
    TbmRecord getTodayTbm();
    List<TbmRecord> getRecentRecords();
    void completeTodayTbm(int participants, String briefing, boolean hasPhoto);
}
