package dev.sbs.minecraftapi.util;

import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.election.SpecialElection;
import dev.sbs.minecraftapi.skyblock.date.Season;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class SkyBlockDateTest {

    @Test
    public void getDate_ok() {
        long givenDate = convertDateToMillis(2024, 4, 22, 7, 0, 0);
        SkyBlockDate currentDate = new SkyBlockDate(givenDate, true);
        //SkyBlockDate currentDate = new SkyBlockDate(System.currentTimeMillis(), true);

        long currentYear = currentDate.getYear();
        long currentMonth = currentDate.getMonth();
        Season currentSeason = currentDate.getSeason();
        long currentDay = currentDate.getDay();
        long currentHour = currentDate.getHour();
        long currentMinute = currentDate.getMinute();
        //long currentSeconds = currentDate.getSecond();
        SkyBlockDate sbDate2 = new SkyBlockDate(currentDate.getYear(), currentDate.getMonth(), currentDate.getDay(), currentDate.getHour(), currentDate.getMinute());

        SkyBlockDate futureDate = new SkyBlockDate(400, 1, 1, 0, 0);
        ConcurrentList<SpecialElection> specialMayors1 = SkyBlockDate.getSpecialMayors(3);
        long remaining = futureDate.getRealTime() - currentDate.getRealTime();
        long seconds = remaining / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        long days = hours / 24;
        hours %= 24;
        minutes %= 60;
        seconds %= 60;

        long dateInMilliseconds = convertDateToMillis(
            2023,
            6, // June
            1,
            0,
            0,
            0
        );
        long elapsedHours = getElapsedHours(dateInMilliseconds);
        long artificialJank = elapsedHours / 124;
        long test2 = 337 - artificialJank;
        SkyBlockDate sbd = new SkyBlockDate(dateInMilliseconds, true);
        int year = sbd.getYear();

        SkyBlockDate sbd2 = new SkyBlockDate(400, 1, 0);
        String time = sbd2.toString();

        SpecialElection nextSpecialMayor = SkyBlockDate.getNextSpecialMayor();
        ConcurrentList<SpecialElection> specialMayors = SkyBlockDate.getSpecialMayors(
            5,
            new SkyBlockDate(System.currentTimeMillis()).append(-16)
        );
        int specialYear = nextSpecialMayor.getVoting().getStart().getYear();

        System.out.println("SB Time #1: " + currentDate.getSkyBlockTime());
        System.out.println("Year #1: " + currentDate.getYear());
        System.out.println("Month #1: " + currentDate.getMonth());
        System.out.println("Day #1: " + currentDate.getDay());
        System.out.println("Hour #1: " + currentDate.getHour());
        System.out.println("Minute #1: " + currentDate.getMinute());
        System.out.println("Season #1: " + currentDate.getSeason().getName());

        System.out.println();

        System.out.println("SB Time #2: " + sbDate2.getSkyBlockTime());
        System.out.println("Year #2: " + sbDate2.getYear());
        System.out.println("Month #2: " + sbDate2.getMonth());
        System.out.println("Day #2: " + sbDate2.getDay());
        System.out.println("Hour #2: " + sbDate2.getHour());
        System.out.println("Minute #2: " + sbDate2.getMinute());
        System.out.println("Season #2: " + sbDate2.getSeason().getName());

        MatcherAssert.assertThat(currentDate, Matchers.equalTo(sbDate2));
    }

    private static long convertDateToMillis(int year, int month, int day, int hours, int minutes, int seconds) {
        return LocalDateTime.of(year, month, day, hours, minutes, seconds)
            .atZone(ZoneId.of("America/New_York"))
            .toInstant()
            .toEpochMilli();
    }

    private static long getElapsedHours(long historicalTime) {
        return Duration.between(Instant.ofEpochMilli(historicalTime), Instant.now()).toHours();
    }

}
