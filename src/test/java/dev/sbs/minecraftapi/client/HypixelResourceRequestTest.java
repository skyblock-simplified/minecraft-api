package dev.sbs.minecraftapi.client;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.util.SystemUtil;
import dev.sbs.minecraftapi.client.hypixel.HypixelClient;
import dev.sbs.minecraftapi.client.hypixel.request.HypixelEndpoint;
import dev.sbs.minecraftapi.client.hypixel.response.resource.ResourceCollectionsResponse;
import dev.sbs.minecraftapi.client.hypixel.response.resource.ResourceItemsResponse;
import dev.sbs.minecraftapi.client.hypixel.response.resource.ResourceSkillsResponse;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class HypixelResourceRequestTest {

    private static final HypixelEndpoint HYPIXEL_RESOURCE_REQUEST;

    static {
        HYPIXEL_RESOURCE_REQUEST = SimplifiedApi.getClient(HypixelClient.class).getEndpoints();
    }

    @Test
    public void getSkills_ok() {
        SimplifiedApi.getKeyManager().add(SystemUtil.getEnvPair("HYPIXEL_API_KEY"));
        ResourceSkillsResponse skills = HYPIXEL_RESOURCE_REQUEST.getSkills();
        MatcherAssert.assertThat(skills.getSkills().size(), Matchers.greaterThan(0));
    }

    @Test
    public void getCollections_ok() {
        ResourceCollectionsResponse collections = HYPIXEL_RESOURCE_REQUEST.getCollections();
        MatcherAssert.assertThat(collections.getCollections().size(), Matchers.greaterThan(0));
    }

    @Test
    public void getItems_ok() {
        ResourceItemsResponse items = HYPIXEL_RESOURCE_REQUEST.getItems();
        MatcherAssert.assertThat(items.getItems().size(), Matchers.greaterThan(0));
    }

}
