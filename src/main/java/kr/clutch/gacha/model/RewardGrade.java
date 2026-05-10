package kr.clutch.gacha.model;

public enum RewardGrade {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY,
    MYTHIC;

    public boolean isAtLeast(RewardGrade other) {
        return ordinal() >= other.ordinal();
    }
}
