package com.example.app;

import java.util.ArrayList;
import java.util.List;

public class MockTbmRepository implements TbmRepository {
    private TbmRecord today = new TbmRecord(
            "TBM-2026-0714-001", "PTW-2026-0714-018", "2026.07.14",
            "B-07 블록 상부 배관 조립", "B-07 · 3층", 0, "", false);
    private final List<TbmRecord> records = new ArrayList<>();

    public MockTbmRepository() {
        records.add(new TbmRecord("TBM-0713-018", "PTW-0713-018", "2026.07.13",
                "고소 배관 조립 작업", "B-07", 8,
                "안전벨트 이중 체결과 하부 통행 차단을 확인했습니다.", true));
        records.add(new TbmRecord("TBM-0712-011", "PTW-0712-011", "2026.07.12",
                "선체 블록 용접 작업", "C-03", 6,
                "화기 감시자 배치 및 소화기 위치를 공유했습니다.", true));
        records.add(new TbmRecord("TBM-0711-007", "PTW-0711-007", "2026.07.11",
                "크레인 양중 작업", "A-12", 11,
                "양중 반경 출입 통제와 신호수 위치를 확인했습니다.", true));
    }

    @Override public boolean login(String employeeNo, String password) {
        return employeeNo != null && password != null
                && !employeeNo.trim().isEmpty() && !password.trim().isEmpty();
    }

    @Override public TbmRecord getTodayTbm() { return today; }

    @Override public List<TbmRecord> getRecentRecords() { return new ArrayList<>(records); }

    @Override public void completeTodayTbm(int participants, String briefing, boolean hasPhoto) {
        today = new TbmRecord(today.id, today.permitId, today.date, today.workName,
                today.block, participants, briefing, true);
        records.removeIf(record -> record.id.equals(today.id));
        records.add(0, today);
    }
}
