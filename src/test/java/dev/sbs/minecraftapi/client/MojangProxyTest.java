package dev.sbs.minecraftapi.client;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.scheduler.Scheduler;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.mojang.MojangProxy;
import dev.sbs.minecraftapi.client.mojang.exception.MojangApiException;
import dev.sbs.minecraftapi.client.mojang.request.MojangEndpoints;
import dev.sbs.minecraftapi.client.mojang.response.MojangMultiUsernameResponse;
import dev.sbs.minecraftapi.client.mojang.response.MojangUsernameResponse;
import dev.sbs.minecraftapi.client.sbs.SbsClient;
import dev.sbs.minecraftapi.client.sbs.request.SbsEndpoints;
import org.junit.jupiter.api.Test;

public class MojangProxyTest {

    @Test
    public void makeMojangRequests_ok() {
        MojangProxy mojangProxy = MinecraftApi.getMojangProxy();
        MojangEndpoints apiRequest = mojangProxy.getEndpoints();
        MojangUsernameResponse usernameResponse = apiRequest.getPlayer("CraftedFury");
        System.out.println(usernameResponse.getUsername() + " : " + usernameResponse.getUniqueId());
    }

    @Test
    public void makeMultiMojangRequests_ok() {
        MojangProxy mojangProxy = MinecraftApi.getMojangProxy();
        MojangEndpoints apiRequest = mojangProxy.getEndpoints();
        MojangMultiUsernameResponse multiUsernameResponse = apiRequest.getMultipleUniqueIds("CraftedFury", "GoldenDusk");
        multiUsernameResponse.getProfiles().forEach(usernameResponse -> System.out.println(usernameResponse.getUsername() + " : " + usernameResponse.getUniqueId()));
    }

    @Test
    public void makeMojangProfile_ok() {
        MojangProxy mojangProxy = MinecraftApi.getMojangProxy();
        //MojangProfileResponse mojangProfileResponse = mojangProxy.getMojangProfile("CraftedFury");
        SbsEndpoints sbsEndpoints = SimplifiedApi.getClient(SbsClient.class).getEndpoints();

        for (int i = 0; i < 1_000; i++) {
            int x = i;
            try {
                SimplifiedApi.getScheduler().scheduleAsync(() -> {
                    MojangUsernameResponse mojangUsernameResponse = sbsEndpoints.getTestProfileFromUsername("CraftedFury");
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
