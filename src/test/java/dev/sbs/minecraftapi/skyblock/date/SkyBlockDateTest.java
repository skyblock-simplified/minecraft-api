package dev.sbs.minecraftapi.skyblock.date;

import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.election.Election;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.election.SpecialElection;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SkyBlockDateTest {

    // year=100, month=6 (LATE_SUMMER), day=15, hour=12
    private static final SkyBlockDate ANCHOR = new SkyBlockDate(100, 6, 15, 12);

    // --- Constructor Round-Trips ---

    @Test
    public void constructor_yearMonthDayHour_roundTrips() {
        SkyBlockDate date = new SkyBlockDate(100, 6, 15, 12);
        assertThat(date.getYear(), is(100));
        assertThat(date.getMonth(), is(6));
        assertThat(date.getDay(), is(15));
        assertThat(date.getHour(), is(12));
    }

    @Test
    public void constructor_yearSeasonDay_roundTrips() {
        SkyBlockDate date = new SkyBlockDate(100, Season.LATE_SUMMER, 15);
        assertThat(date.getYear(), is(100));
        assertThat(date.getMonth(), is(6));
        assertThat(date.getDay(), is(15));
        assertThat(date.getSeason(), is(Season.LATE_SUMMER));
    }

    @Test
    public void constructor_realTimeMillis_matchesComponents() {
        SkyBlockDate date = new SkyBlockDate(SkyBlockDate.Launch.SKYBLOCK);
        assertThat(date.getYear(), is(1));
        assertThat(date.getMonth(), is(1));
        assertThat(date.getDay(), is(1));
    }

    @Test
    public void constructor_skyBlockMillis_interpretsCorrectly() {
        long skyBlockMillis = ANCHOR.getSkyBlockTime();
        SkyBlockDate date = new SkyBlockDate(skyBlockMillis, false);
        assertThat(date.getYear(), is(ANCHOR.getYear()));
        assertThat(date.getMonth(), is(ANCHOR.getMonth()));
        assertThat(date.getDay(), is(ANCHOR.getDay()));
        assertThat(date.getHour(), is(ANCHOR.getHour()));
    }

    // --- Getters ---

    @Test
    public void getYear_returnsCorrectYear() {
        assertThat(ANCHOR.getYear(), is(100));
    }

    @Test
    public void getMonth_returnsCorrectMonth() {
        assertThat(ANCHOR.getMonth(), is(6));
    }

    @Test
    public void getDay_returnsCorrectDay() {
        assertThat(ANCHOR.getDay(), is(15));
    }

    @Test
    public void getHour_returnsCorrectHour() {
        assertThat(ANCHOR.getHour(), is(12));
    }

    @Test
    public void getMinute_defaultsToZero() {
        assertThat(ANCHOR.getMinute(), is(0));
    }

    @Test
    public void getSeason_matchesMonth() {
        assertThat(ANCHOR.getSeason(), is(Season.LATE_SUMMER));
    }

    @Test
    public void getSecond_throwsUnsupportedOperation() {
        assertThrows(UnsupportedOperationException.class, ANCHOR::getSecond);
    }

    @Test
    public void getSkyBlockTime_isRealTimeMinusEpoch() {
        assertThat(ANCHOR.getSkyBlockTime(), is(ANCHOR.getRealTime() - SkyBlockDate.Launch.SKYBLOCK));
    }

    // --- Add Methods ---

    @Test
    public void add_year_incrementsYear() {
        SkyBlockDate result = ANCHOR.add(5);
        assertThat(result.getYear(), is(105));
        assertThat(result.getMonth(), is(6));
        assertThat(result.getDay(), is(15));
        assertThat(result.getHour(), is(12));
    }

    @Test
    public void add_yearMonth_incrementsBoth() {
        SkyBlockDate result = ANCHOR.add(2, 3);
        assertThat(result.getYear(), is(102));
        assertThat(result.getMonth(), is(9));
    }

    @Test
    public void add_yearMonthDay_incrementsAll() {
        SkyBlockDate result = ANCHOR.add(1, 2, 10);
        assertThat(result.getYear(), is(101));
        assertThat(result.getMonth(), is(8));
        assertThat(result.getDay(), is(25));
    }

    @Test
    public void add_yearMonthDayHour_incrementsAll() {
        SkyBlockDate result = ANCHOR.add(1, 1, 5, 6);
        assertThat(result.getYear(), is(101));
        assertThat(result.getMonth(), is(7));
        assertThat(result.getDay(), is(20));
        assertThat(result.getHour(), is(18));
    }

    @Test
    public void add_yearSeason_delegatesToMonthOverload() {
        SkyBlockDate withSeason = ANCHOR.add(1, Season.SPRING);
        SkyBlockDate withMonth = ANCHOR.add(1, 2);
        assertThat(withSeason, equalTo(withMonth));
    }

    // --- Subtract Methods ---

    @Test
    public void subtract_year_decrementsYear() {
        SkyBlockDate result = ANCHOR.subtract(5);
        assertThat(result.getYear(), is(95));
    }

    @Test
    public void subtract_yearMonth_decrementsBoth() {
        SkyBlockDate result = ANCHOR.subtract(2, 3);
        assertThat(result.getYear(), is(98));
        assertThat(result.getMonth(), is(3));
    }

    @Test
    public void subtract_yearMonthDay_decrementsAll() {
        SkyBlockDate result = ANCHOR.subtract(1, 2, 5);
        assertThat(result.getYear(), is(99));
        assertThat(result.getMonth(), is(4));
        assertThat(result.getDay(), is(10));
    }

    @Test
    public void subtract_yearMonthDayHour_decrementsAll() {
        SkyBlockDate result = ANCHOR.subtract(1, 1, 5, 6);
        assertThat(result.getYear(), is(99));
        assertThat(result.getMonth(), is(5));
        assertThat(result.getDay(), is(10));
        assertThat(result.getHour(), is(6));
    }

    // --- Equals / HashCode ---

    @Test
    public void equals_sameComponents_areEqual() {
        SkyBlockDate a = new SkyBlockDate(100, 6, 15, 12);
        SkyBlockDate b = new SkyBlockDate(100, 6, 15, 12);
        assertThat(a, equalTo(b));
    }

    @Test
    public void equals_differentComponents_areNotEqual() {
        SkyBlockDate different = new SkyBlockDate(101, 6, 15, 12);
        assertThat(ANCHOR, not(equalTo(different)));
    }

    @Test
    public void hashCode_sameComponents_sameHash() {
        SkyBlockDate a = new SkyBlockDate(100, 6, 15, 12);
        SkyBlockDate b = new SkyBlockDate(100, 6, 15, 12);
        assertThat(a.hashCode(), is(b.hashCode()));
    }

    // --- Immutability ---

    @Test
    public void add_doesNotMutateOriginal() {
        SkyBlockDate original = new SkyBlockDate(100, 6, 15, 12);
        original.add(1);
        assertThat(original.getYear(), is(100));
    }

    @Test
    public void subtract_doesNotMutateOriginal() {
        SkyBlockDate original = new SkyBlockDate(100, 6, 15, 12);
        original.subtract(1);
        assertThat(original.getYear(), is(100));
    }

    // --- Static Methods ---

    @Test
    public void getRealTimeStatic_seasonOverloads_delegateCorrectly() {
        long one = SkyBlockDate.getRealTime(Season.EARLY_SPRING);
        long two = SkyBlockDate.getRealTime(Season.EARLY_SPRING, 1);
        long three = SkyBlockDate.getRealTime(Season.EARLY_SPRING, 1, 1);
        long four = SkyBlockDate.getRealTime(Season.EARLY_SPRING, 1, 1, 0);
        assertThat(one, is(two));
        assertThat(two, is(three));
        assertThat(three, is(four));
    }

    @Test
    public void getSkyBlockTimeStatic_isRealTimeMinusEpoch() {
        long realTime = SkyBlockDate.getRealTime(Season.EARLY_SPRING, 15, 12, 0);
        long skyBlockTime = SkyBlockDate.getSkyBlockTime(Season.EARLY_SPRING, 15, 12, 0);
        assertThat(skyBlockTime, is(realTime - SkyBlockDate.Launch.SKYBLOCK));
    }

    // --- Inner Classes ---

    @Test
    public void realTime_constructsFromEpoch() {
        SkyBlockDate.RealTime date = new SkyBlockDate.RealTime(SkyBlockDate.Launch.SKYBLOCK);
        assertThat(date.getYear(), is(1));
        assertThat(date.getMonth(), is(1));
        assertThat(date.getDay(), is(1));
    }

    @Test
    public void skyBlockTime_constructsFromSeconds() {
        SkyBlockDate.SkyBlockTime date = new SkyBlockDate.SkyBlockTime(0);
        assertThat(date.getYear(), is(1));
    }

    // --- Election Methods ---

    @Test
    public void getMayors_returnsRequestedCount() {
        SkyBlockDate fromDate = new SkyBlockDate(1, 1, 1, 0);
        ConcurrentList<Election> mayors = SkyBlockDate.getMayors(3, fromDate);
        assertThat(mayors.size(), is(3));
    }

    @Test
    public void getMayors_spacedOneYearApart() {
        SkyBlockDate fromDate = new SkyBlockDate(1, 1, 1, 0);
        ConcurrentList<Election> mayors = SkyBlockDate.getMayors(3, fromDate);
        assertThat(mayors.get(1).getYear() - mayors.get(0).getYear(), is(1));
        assertThat(mayors.get(2).getYear() - mayors.get(1).getYear(), is(1));
    }

    @Test
    public void getSpecialMayors_cyclesThroughSpecialMayorCycle() {
        SkyBlockDate fromDate = new SkyBlockDate(1, 1, 1, 0);
        ConcurrentList<SpecialElection> specials = SkyBlockDate.getSpecialMayors(3, fromDate);
        assertThat(specials.get(0).getSpecialMayor(), is("SCORPIUS"));
        assertThat(specials.get(1).getSpecialMayor(), is("DERPY"));
        assertThat(specials.get(2).getSpecialMayor(), is("JERRY"));
    }

    // --- Length Constants ---

    @Test
    public void lengthConstants_areConsistent() {
        assertThat(SkyBlockDate.Length.YEAR_MS, is(SkyBlockDate.Length.MONTHS_TOTAL * SkyBlockDate.Length.MONTH_MS));
        assertThat(SkyBlockDate.Length.MONTH_MS, is(SkyBlockDate.Length.DAYS_TOTAL * SkyBlockDate.Length.DAY_MS));
        assertThat(SkyBlockDate.Length.DAY_MS, is(SkyBlockDate.Length.HOURS_TOTAL * SkyBlockDate.Length.HOUR_MS));
        assertThat(SkyBlockDate.Length.HOUR_MS, is((long) (SkyBlockDate.Length.MINUTES_TOTAL * SkyBlockDate.Length.MINUTE_MS)));
    }

}
