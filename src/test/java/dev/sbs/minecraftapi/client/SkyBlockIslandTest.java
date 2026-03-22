package dev.sbs.minecraftapi.client;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.JpaConfig;
import dev.sbs.api.persistence.JpaSession;
import dev.sbs.api.tuple.pair.Pair;
import dev.sbs.minecraftapi.client.hypixel.HypixelClient;
import dev.sbs.minecraftapi.client.hypixel.exception.HypixelApiException;
import dev.sbs.minecraftapi.client.hypixel.request.HypixelEndpoint;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
public class SkyBlockIslandTest {

    @Test
    public void getGuildLevels_ok() {
        try {
            System.out.println("Database Starting... ");
            JpaSession session = SimplifiedApi.getSessionManager().connect(JpaConfig.defaultSql());
            System.out.println("Database initialized in " + session.getInitialization().getDurationMillis() + "ms");
            System.out.println("Database started in " + session.getRepositoryCache().getDurationMillis() + "ms");
            HypixelEndpoint hypixelEndpoints = SimplifiedApi.getClient(HypixelClient.class).getEndpoint();
            String guildName = "SkyBlock Simplified";

            hypixelEndpoints.getGuildByName(guildName)
                .getGuild()
                .ifPresent(guild -> {
                    ConcurrentMap<Pair<UUID, String>, Integer> playerLevels = guild.getMembers()
                        .stream()
                        .map(member -> hypixelEndpoints.getProfiles(member.getUniqueId())
                            .getIslands()
                            .stream()
                            .max(Comparator.comparingInt(island -> island.getMembers()
                                .get(member.getUniqueId())
                                .getLeveling()
                                .getExperience()
                            ))
                            .map(island -> Pair.of(
                                Pair.of(
                                    member.getUniqueId(),
                                    hypixelEndpoints.getPlayer(member.getUniqueId()).getPlayer().get().getDisplayName()
                                ),
                                (int) island.getMembers()
                                    .get(member.getUniqueId())
                                    .getLeveling()
                                    .getLevel()
                            ))
                        )
                        .flatMap(Optional::stream)
                        .sorted(Comparator.comparingInt(Pair::getRight))
                        .collect(Concurrent.toMap());

                    playerLevels.forEach((pair, level) -> System.out.println(pair.getRight() + ": " + level + " (" + pair.getLeft().toString() + ")"));
                    System.out.println("---");
                    double averageLevel = playerLevels.stream()
                        .filter(entry -> entry.getValue() > 0)
                        .mapToDouble(Map.Entry::getValue)
                        .sum() / playerLevels.size();
                    long belowAverage = playerLevels.stream()
                        .filter(entry -> entry.getValue() > 0)
                        .map(Map.Entry::getValue)
                        .filter(level -> level <= averageLevel)
                        .count();
                    long below200 = playerLevels.stream()
                        .filter(entry -> entry.getValue() > 0)
                        .map(Map.Entry::getValue)
                        .filter(level -> level <= 200)
                        .count();
                    System.out.println("Average Level: " + averageLevel + " / Below Average: " + belowAverage + " / Below 200: " + below200);
                    System.out.println("---");
                    playerLevels.stream()
                        .filter(entry -> entry.getValue() == 0)
                        .forEach(entry -> System.out.println(entry.getKey().getRight() + ": " + entry.getValue() + " (" + entry.getKey().getLeft().toString() + ")"));
                });
        } catch (HypixelApiException hypixelApiException) {
            hypixelApiException.printStackTrace();
            MatcherAssert.assertThat(hypixelApiException.getStatus().getCode(), Matchers.greaterThan(400));
        } catch (Exception exception) {
            exception.printStackTrace();
            Assertions.fail();
        } finally {
            SimplifiedApi.getSessionManager().disconnect();
        }
    }

    /*@Test
    public void getPlayerStats_ok() {
        try {
            System.out.println("Database Starting... ");
            JpaSession session = SimplifiedApi.getSessionManager().connect(JpaConfig.defaultSql());
            System.out.println("Database initialized in " + session.getInitialization().getDurationMillis() + "ms");
            System.out.println("Database started in " + session.getRepositoryCache().getDurationMillis() + "ms");
            HypixelEndpoint hypixelEndpoints = SimplifiedApi.getClient(HypixelClient.class).getEndpoints();

            UUID uniqueId = StringUtil.toUUID("f33f51a7-9691-4076-abda-f66e3d047a71"); // CraftedFury
            //UUID uniqueId = StringUtil.toUUID("df5e1701-809c-48be-9b0d-ef50b83b009e"); // GoldenDusk
            SkyBlockProfilesResponse profiles = hypixelEndpoints.getProfiles(uniqueId);

            // Did Hypixel Reply
            MatcherAssert.assertThat(profiles.isSuccess(), Matchers.equalTo(true));
            SkyBlockIsland island = profiles.getSelected(); // Bingo Profile = 0

            // Does Member Exist
            MatcherAssert.assertThat(island.getMembers().containsKey(uniqueId), Matchers.equalTo(true));
            SkyBlockMember member = island.getMembers().get(uniqueId);

            // SkyBlock Levels
            int exp1 = member.getLeveling().getExperience();
            int explevel = member.getLeveling().getLevel();

            Bestiary bestiaryData = member.getBestiary();

            ProfileStats profileStats = island.getProfileStats(member);
            profileStats.getStats(ProfileStats.Type.ACCESSORY_POWER)
                .stream()
                .forEach(entry -> System.out.println(
                    entry.getKey().getKey() + ": " +
                        entry.getValue().getTotal() + " (" + entry.getValue().getBase() + " / " + entry.getValue().getBonus() + ")")
                );

            System.out.println("All Accessories: " + profileStats.getAccessoryBag().getAccessories().size());
            System.out.println("Filtered Accessories: " + profileStats.getAccessoryBag().getFilteredAccessories().size());
            System.out.println("Magic Power: " + profileStats.getAccessoryBag().getMagicalPower());
            System.out.println("\n---\n");

            profileStats.getAccessoryBag()
                .getFilteredAccessories()
                .sorted(ObjectData::getRarity)
                .stream()
                .collect(Collectors.groupingBy(accessory -> accessory.getRarity().getKey(), Collectors.counting()))
                .forEach((rarity, count) -> System.out.println(rarity + ": " + count));

            System.out.println("\n---\n");

            profileStats.getAccessoryBag()
                .getFilteredAccessories()
                .sorted(ObjectData::getRarity)
                .stream()
                .map(accessory -> accessory.getAccessory().getItem().getItemId() + ": " + accessory.getRarity().getName() + " (" + accessory.getRarity().getMagicPowerMultiplier() + ")")
                .forEach(System.out::println);

            assert exp1 > 0;
            // Player Stats
            //PlayerStats playerStats = island.getPlayerStats(member);

            //playerStats.getStats(PlayerStats.Type.ACTIVE_PET)
            //    .forEach((statModel, statData) -> System.out.println("PET: " + statModel.getKey() + ": " + statData.getTotal() + " (" + statData.getBase() + " / " + statData.getBonus() + ")"));

            //playerStats.getCombinedStats().forEach((statModel, statData) -> System.out.println(statModel.getKey() + ": " + statData.getTotal() + " (" + statData.getBase() + " / " + statData.getBonus() + ")"));
        } catch (HypixelApiException hypixelApiException) {
            hypixelApiException.printStackTrace();
            MatcherAssert.assertThat(hypixelApiException.getStatus().getCode(), Matchers.greaterThan(400));
        } catch (Exception exception) {
            exception.printStackTrace();
            Assertions.fail();
        } finally {
            SimplifiedApi.getSessionManager().disconnect();
        }
    }

    @Test
    public void getIsland_ok() {
        try {
            System.out.println("Database Starting... ");
            JpaSession session = SimplifiedApi.getSessionManager().connect(JpaConfig.defaultSql());
            System.out.println("Database initialized in " + session.getInitialization().getDurationMillis() + "ms");
            System.out.println("Database started in " + session.getRepositoryCache().getDurationMillis() + "ms");
            HypixelEndpoint hypixelEndpoints = SimplifiedApi.getClient(HypixelClient.class).getEndpoints();
            ProfileModel pineappleProfile = SimplifiedApi.getRepository(ProfileModel.class).findFirstOrNull(ProfileModel::getKey, "PINEAPPLE");
            ProfileModel bananaProfile = SimplifiedApi.getRepository(ProfileModel.class).findFirstOrNull(ProfileModel::getKey, "BANANA");

            Pair<UUID, ProfileModel> pair_CraftedFury = Pair.of(
                StringUtil.toUUID("f33f51a7-9691-4076-abda-f66e3d047a71"),
                SimplifiedApi.getRepository(ProfileModel.class).findFirstOrNull(ProfileModel::getKey, "PINEAPPLE")
            );
            Pair<UUID, ProfileModel> pair_GoldenDusk = Pair.of(
                StringUtil.toUUID("df5e1701-809c-48be-9b0d-ef50b83b009e"),
                SimplifiedApi.getRepository(ProfileModel.class).findFirstOrNull(ProfileModel::getKey, "POMEGRANATE")
            );
            Pair<UUID, ProfileModel> pair_CrazyHjonk = Pair.of(
                StringUtil.toUUID("c360fb57-1e6c-458c-86b5-c971e864536c"),
                SimplifiedApi.getRepository(ProfileModel.class).findFirstOrNull(ProfileModel::getKey, "BANANA")
            );
            Pair<UUID, ProfileModel> checkThis = pair_CraftedFury;

            SkyBlockProfilesResponse profiles = hypixelEndpoints.getProfiles(checkThis.getKey());
            MatcherAssert.assertThat(profiles.isSuccess(), Matchers.equalTo(true));

            //SkyBlockIsland island = profiles.getSelected(); // Bingo Profile = 0
            Optional<SkyBlockIsland> pineappleIsland = profiles.getIsland(checkThis.getRight().getKey());
            assert pineappleIsland.isPresent();
            SkyBlockIsland island = pineappleIsland.get();
            SkyBlockMember member = island.getMembers().get(checkThis.getKey());
            EnhancedMember enhancedMember = member.asEnhanced();

            int petScore = member.getPets().getPetScore();

            /*int uniques = island.getMinions()
                .stream()
                .mapToInt(minion -> minion.getUnlocked().size())
                .sum();*/

            // skills, skill_levels, slayers, slayer_levels, dungeons, dungeon_classes, dungeon_levels
            /*double skillAverage = member.getSkills().getAverage();
            ConcurrentMap<SkillProgress.Entry, Weight> skillWeights = member.getSkills().getWeight();
            ConcurrentMap<SlayerProgress.Boss, Weight> slayerWeights = member.getSlayers().getWeight();
            ConcurrentMap<DungeonEntry, Weight> dungeonWeights = member.getDungeons().getWeight();
            ConcurrentMap<DungeonClass, Weight> dungeonClassWeights = member.getDungeons().getClassWeight();
            ConcurrentList<JacobsContest.Contest> contests = member.getJacobsContest().getContests();

            // skills, skill_levels
            Repository<SkillModel> skillRepo = SimplifiedApi.getRepository(SkillModel.class);
            SkillModel combatSkillModel = skillRepo.findFirstOrNull(SkillModel::getKey, "COMBAT");
            ConcurrentList<SkillLevelModel> skillLevels = SimplifiedApi.getRepository(SkillLevelModel.class)
                .findAll(SkillLevelModel::getSkill, combatSkillModel)
                .collect(Concurrent.toList());
            MatcherAssert.assertThat(skillLevels.size(), Matchers.equalTo(60));

            // collection_items, collections
            Repository<CollectionModel> collectionRepo = SimplifiedApi.getRepository(CollectionModel.class);
            CollectionModel collectionModel = collectionRepo.findFirstOrNull(CollectionModel::getKey, combatSkillModel.getKey());
            Collection sbCollection = member.asEnhanced().getCollection(collectionModel);
            SackModel sbSack = SimplifiedApi.getRepository(SackModel.class).findFirstOrNull(SackModel::getKey, "MINING");
            //MatcherAssert.assertThat(member.getSack(sbSack).getStored().size(), Matchers.greaterThan(0));

            // minion_tier_upgrades, minion_tiers, items
            MinionTierUpgradeModel wheatGen11 = SimplifiedApi.getRepository(MinionTierUpgradeModel.class).findFirstOrNull(
                SearchFunction.combine(MinionTierUpgradeModel::getMinionTier, SearchFunction.combine(MinionTierModel::getItem, ItemModel::getItemId)), "WHEAT_GENERATOR_11");

            MatcherAssert.assertThat(wheatGen11.getMinionTier().getMinion().getKey(), Matchers.equalTo("WHEAT"));
            MatcherAssert.assertThat(wheatGen11.getItemCost().getItemId(), Matchers.equalTo("ENCHANTED_HAY_BLOCK"));

            // rarities, pets, pet_items, pet_exp_scales
            ConcurrentList<PetProgress.Entry> pets = member.getPets().getPets();
            Optional<PetProgress.Entry> optionalSpiderPet = pets.stream().filter(pet -> pet.getType().equals("SPIDER")).findFirst();
            Optional<PetProgress.Entry> optionalDragonPet = pets.stream().filter(pet -> pet.getType().equals("ENDER_DRAGON")).findFirst();

            Optional<PetProgress.Entry> optionalTestPet = pets.stream().filter(pet -> pet.getType().equals("BEE")).findFirst();
            optionalTestPet.ifPresent(testPet -> {
                EnhancedPet atestPet = testPet.asEnhanced();
                ConcurrentList<Double> tiers = atestPet.getExperienceTiers();
                double tierSum = tiers.stream().mapToDouble(value -> value).sum();
                double testExperience = testPet.getExperience();
                double testX1 = atestPet.getProgressExperience();
                double testX2 = atestPet.getNextExperience();
                double testX3 = atestPet.getMissingExperience();
                double testX4 = atestPet.getTotalProgressPercentage();
                int testLevel = atestPet.getLevel();
                int testRawLevel = atestPet.getRawLevel();
                int testMaxLevel = atestPet.getMaxLevel();
                double testPercentage = atestPet.getProgressPercentage();
                String stop = "here";
            });

            optionalSpiderPet.ifPresent(spiderPetInfo -> optionalDragonPet.ifPresent(dragInfo -> {
                EnhancedPet espiderPetInfo = spiderPetInfo.asEnhanced();
                espiderPetInfo.getHeldItemModel().ifPresent(itemModel -> MatcherAssert.assertThat(itemModel.getRarity().getOrdinal(), Matchers.greaterThanOrEqualTo(0)));

                double spiderExp = spiderPetInfo.getExperience();
                MatcherAssert.assertThat(spiderExp, Matchers.greaterThan(0.0));
                int spiderLevel = espiderPetInfo.getLevel();
                MatcherAssert.assertThat(spiderLevel, Matchers.equalTo(100));

                PetModel spiderPet = espiderPetInfo.getTypeModel().get();
                PetModel dragPet = dragInfo.asEnhanced().getTypeModel().get();

                RarityModel commonRarity = SimplifiedApi.getRepository(RarityModel.class).findFirstOrNull(RarityModel::getKey, "COMMON");
                MatcherAssert.assertThat(spiderPet.getLowestRarity(), Matchers.equalTo(commonRarity));

                int wolf_hs = spiderPet.hashCode();
                int drag_hs = dragPet.hashCode();

                MatcherAssert.assertThat(wolf_hs, Matchers.not(drag_hs));
            }));

            MatcherAssert.assertThat(member.getUniqueId(), Matchers.equalTo(checkThis.getKey()));
        } catch (HypixelApiException hypixelApiException) {
            hypixelApiException.printStackTrace();
            MatcherAssert.assertThat(hypixelApiException.getStatus().getCode(), Matchers.greaterThan(400));
        } catch (Exception exception) {
            exception.printStackTrace();
            Assertions.fail();
        } finally {
            SimplifiedApi.getSessionManager().disconnect();
        }
    }*/

}
