package dev.sbs.minecraftapi.client;

import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.mojang.MojangContract;
import dev.sbs.minecraftapi.client.mojang.response.MojangMultiUsername;
import dev.sbs.minecraftapi.client.mojang.response.MojangUsername;
import dev.simplified.client.Proxy;
import org.junit.jupiter.api.Test;

public class MojangProxyTest {

    @Test
    public void makeMojangRequests_ok() {
        Proxy<MojangContract> mojangProxy = MinecraftApi.getMojangProxy();
        MojangContract mojang = mojangProxy.getContract();
        MojangUsername usernameResponse = mojang.getPlayer("CraftedFury");
        System.out.println(usernameResponse.getUsername() + " : " + usernameResponse.getUniqueId());
    }

    @Test
    public void makeMultiMojangRequests_ok() {
        Proxy<MojangContract> mojangProxy = MinecraftApi.getMojangProxy();
        MojangContract mojang = mojangProxy.getContract();
        MojangMultiUsername multiUsernameResponse = mojang.getMultipleUniqueIds("CraftedFury", "GoldenDusk");
        multiUsernameResponse.getProfiles().forEach(usernameResponse -> System.out.println(usernameResponse.getUsername() + " : " + usernameResponse.getUniqueId()));
    }

}
