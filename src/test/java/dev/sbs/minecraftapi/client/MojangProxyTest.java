package dev.sbs.minecraftapi.client;

import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.mojang.MojangProxy;
import dev.sbs.minecraftapi.client.mojang.request.MojangEndpoint;
import dev.sbs.minecraftapi.client.mojang.response.MojangMultiUsername;
import dev.sbs.minecraftapi.client.mojang.response.MojangUsername;
import org.junit.jupiter.api.Test;

public class MojangProxyTest {

    @Test
    public void makeMojangRequests_ok() {
        MojangProxy mojangProxy = MinecraftApi.getMojangProxy();
        MojangEndpoint apiRequest = mojangProxy.getEndpoint();
        MojangUsername usernameResponse = apiRequest.getPlayer("CraftedFury");
        System.out.println(usernameResponse.getUsername() + " : " + usernameResponse.getUniqueId());
    }

    @Test
    public void makeMultiMojangRequests_ok() {
        MojangProxy mojangProxy = MinecraftApi.getMojangProxy();
        MojangEndpoint apiRequest = mojangProxy.getEndpoint();
        MojangMultiUsername multiUsernameResponse = apiRequest.getMultipleUniqueIds("CraftedFury", "GoldenDusk");
        multiUsernameResponse.getProfiles().forEach(usernameResponse -> System.out.println(usernameResponse.getUsername() + " : " + usernameResponse.getUniqueId()));
    }

}
