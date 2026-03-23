package dev.sbs.minecraftapi.client;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.scheduler.Scheduler;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.mojang.MojangProxy;
import dev.sbs.minecraftapi.client.mojang.exception.MojangApiException;
import dev.sbs.minecraftapi.client.mojang.request.MojangEndpoint;
import dev.sbs.minecraftapi.client.mojang.response.MojangMultiUsername;
import dev.sbs.minecraftapi.client.mojang.response.MojangUsername;
import dev.sbs.minecraftapi.client.sbs.SbsClient;
import dev.sbs.minecraftapi.client.sbs.request.SbsEndpoint;
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

    @Test
    public void makeMojangProfile_ok() {
        MojangProxy mojangProxy = MinecraftApi.getMojangProxy();
        //MojangProfileResponse mojangProfileResponse = mojangProxy.getMojangProfile("CraftedFury");
        SbsEndpoint sbsEndpoints = SimplifiedApi.getClient(SbsClient.class).getEndpoint();

        for (int i = 0; i < 1_000; i++) {
            int x = i;
            try {
                SimplifiedApi.getScheduler().scheduleAsync(() -> {
                    MojangUsername mojangUsernameResponse = sbsEndpoints.getTestProfileFromUsername("CraftedFury");
                    System.out.println("Finished request #" + x);
                });
            } catch (MojangApiException mojangApiException) {
                mojangApiException.printStackTrace();
            }
        }

        while (SimplifiedApi.getScheduler().getTasks().notEmpty())
            Scheduler.sleep(1);

        //System.out.println(mojangProfileResponse.getUsername() + " : " + mojangProfileResponse.getUniqueId());
        //String json = SimplifiedApi.getGson().toJson(mojangProfileResponse);
        //System.out.println(json);
    }

}
