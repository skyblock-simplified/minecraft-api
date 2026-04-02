package dev.sbs.minecraftapi.client;

import com.google.gson.Gson;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockIsland;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockMember;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockProfiles;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.Experimentation;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson.TrophyFish;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson.TrophyFishing;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.hoppity.ChocolateFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestSkyBlockIsland {

    @Test
    public void deserializeCraftedFury() throws IOException {
        Gson gson = MinecraftApi.getGson();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("craftedfury.json")) {
            assertThat("craftedfury.json must be on the classpath", is, notNullValue());

            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                SkyBlockProfiles profiles = gson.fromJson(reader, SkyBlockProfiles.class);

                assertThat("profiles should not be null", profiles, notNullValue());
                assertThat("success should be true", profiles.isSuccess(), is(true));
                assertThat("islands list should not be empty", profiles.getIslands().size(), greaterThan(0));

                SkyBlockIsland island = profiles.getSelected();
                assertThat("selected island should not be null", island, notNullValue());
                assertThat("island should have members", island.getMembers().size(), greaterThan(0));

                SkyBlockMember member = island.getMembers().values().iterator().next();

                // Dungeon tier completions (validates Integer-key @Lenient fix)
                assertThat("dungeons map should not be empty", member.getDungeons().getDungeons().size(), greaterThan(0));

                // Rabbit collection (@Lenient + @Extract for nested objects)
                ChocolateFactory chocolateFactory = member.getChocolateFactory();
                assertThat("rabbits should not be empty", chocolateFactory.getRabbits().size(), greaterThan(0));
                assertThat("rabbit eggs should not be empty", chocolateFactory.getEggs().size(), greaterThan(0));

                // Experimentation (@Capture catch-all on Table)
                Experimentation experimentation = member.getExperimentation();
                assertThat("experimentation should not be null", experimentation, notNullValue());
                Experimentation.Table pairings = experimentation.getSuperpairs();
                assertThat("pairings should not be null", pairings, notNullValue());
                assertThat("pairings lastAttempt should not be null", pairings.getLastAttempt(), notNullValue());
            }
        }
    }

    @Test
    public void deserializeTrophyFishing() throws IOException {
        Gson gson = MinecraftApi.getGson();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("craftedfury.json")) {
            assertThat("craftedfury.json must be on the classpath", is, notNullValue());

            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                SkyBlockProfiles profiles = gson.fromJson(reader, SkyBlockProfiles.class);
                SkyBlockMember member = profiles.getSelected().getMembers().values().iterator().next();
                TrophyFishing trophyFish = member.getTrophyFish();

                assertThat("trophy fish should not be null", trophyFish, notNullValue());

                // Known fields
                assertThat("total caught should be positive", trophyFish.getTotalCaught(), greaterThan(0));
                assertThat("rewards should not be empty", trophyFish.getRewards(), not(empty()));

                // @Split on last_caught
                assertThat("last caught should be present", trophyFish.getLastCaught().isPresent(), is(true));
                assertThat("last caught fish should be a Fish", trophyFish.getLastCaught().getLeft(), notNullValue());
                assertThat("last caught tier should be a Tier", trophyFish.getLastCaught().getRight(), notNullValue());

                // @Capture with bare-entry grouping
                assertThat("fish map should contain all 18 fish", trophyFish.getFish().size(), is(TrophyFish.values().length));

                for (TrophyFish fish : TrophyFish.values()) {
                    assertThat(fish.name() + " should exist", trophyFish.getFish(), hasKey(fish));
                    TrophyFishing.TierData tierData = trophyFish.getFish().get(fish);
                    assertThat(fish.name() + " total should be non-negative", tierData.getTotal(), greaterThanOrEqualTo(0));
                    assertThat(fish.name() + " bronze should be non-negative", tierData.getBronze(), greaterThanOrEqualTo(0));
                    assertThat(fish.name() + " silver should be non-negative", tierData.getSilver(), greaterThanOrEqualTo(0));
                    assertThat(fish.name() + " gold should be non-negative", tierData.getGold(), greaterThanOrEqualTo(0));
                    assertThat(fish.name() + " diamond should be non-negative", tierData.getDiamond(), greaterThanOrEqualTo(0));
                }
            }
        }
    }

}
