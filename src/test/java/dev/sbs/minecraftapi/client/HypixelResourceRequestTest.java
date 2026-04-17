package dev.sbs.minecraftapi.client;

import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.hypixel.HypixelContract;
import dev.sbs.minecraftapi.client.hypixel.response.resource.ResourceCollections;
import dev.sbs.minecraftapi.client.hypixel.response.resource.ResourceItems;
import dev.sbs.minecraftapi.client.hypixel.response.resource.ResourceSkills;
import dev.simplified.util.SystemUtil;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class HypixelResourceRequestTest {

    private static final HypixelContract HYPIXEL_RESOURCE_REQUEST;

    static {
        HYPIXEL_RESOURCE_REQUEST = MinecraftApi.getClient(HypixelContract.class).getContract();
    }

    @Test
    public void getSkills_ok() {
        MinecraftApi.getKeyManager().add(SystemUtil.getEnvPair("HYPIXEL_API_KEY"));
        ResourceSkills skills = HYPIXEL_RESOURCE_REQUEST.getSkills();
        MatcherAssert.assertThat(skills.getSkills().size(), Matchers.greaterThan(0));
    }

    @Test
    public void getCollections_ok() {
        ResourceCollections collections = HYPIXEL_RESOURCE_REQUEST.getCollections();
        MatcherAssert.assertThat(collections.getCollections().size(), Matchers.greaterThan(0));
    }

    @Test
    public void getItems_ok() {
        ResourceItems items = HYPIXEL_RESOURCE_REQUEST.getItems();
        MatcherAssert.assertThat(items.getItems().size(), Matchers.greaterThan(0));
    }

}
