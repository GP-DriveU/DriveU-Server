package com.driveu.server.domain.semester.domain;

public enum Term {
    SPRING, SUMMER, FALL, WINTER;

    public static Term fromMonth(int month) {
        if (month >= 3 && month <= 6) return SPRING;
        if (month >= 7 && month <= 8) return SUMMER;
        if (month >= 9 && month <= 12) return FALL;
        return WINTER; // 1, 2월
    }
}
