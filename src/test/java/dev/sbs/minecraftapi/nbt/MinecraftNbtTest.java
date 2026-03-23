package dev.sbs.minecraftapi.nbt;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.util.SystemUtil;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.hypixel.HypixelClient;
import dev.sbs.minecraftapi.client.hypixel.request.HypixelEndpoint;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockAuction;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockAuctions;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

public class MinecraftNbtTest {

    private static String ARMOR = "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" +
        "CZmchn+ilXPn/62Rn47W97EEfoGWyX/VjMdAAA=";

    @Test
    public void getArmor_ok() {
        NbtFactory nbtFactory = MinecraftApi.getNbtFactory();
        CompoundTag compoundTag = nbtFactory.fromBase64(ARMOR);
        CompoundTag armorTag = (CompoundTag) compoundTag.getListTag("i").get(0);

        String json = nbtFactory.toJson(armorTag);
        String snbt = nbtFactory.toSnbt(armorTag);
        CompoundTag item = nbtFactory.fromSnbt(snbt);
        boolean compare = armorTag.equals(item);
        String breakpoint = "here";
    }

    @Test
    public void getAuctionHouse_ok() {
        SimplifiedApi.getKeyManager().add(SystemUtil.getEnvPair("HYPIXEL_API_KEY"));

        HypixelEndpoint hypixelEndpoints = SimplifiedApi.getClient(HypixelClient.class).getEndpoint();
        SkyBlockAuctions auctionsResponse = hypixelEndpoints.getAuctions();
        ConcurrentList<SkyBlockAuction> auctions = Concurrent.newList(auctionsResponse.getAuctions());

        long start = System.currentTimeMillis();
        ConcurrentList<SkyBlockAuction> auctions2 = IntStream.range(1, auctionsResponse.getTotalPages())
            .parallel()
            .mapToObj(page -> hypixelEndpoints.getAuctions(page).getAuctions())
            .flatMap(ConcurrentList::parallelStream)
            .collect(Concurrent.toList());
        long end = System.currentTimeMillis();
        System.out.println("Network test took: " + (end - start) + "ms");
        auctions.addAll(auctions2);

        /*long start2 = System.currentTimeMillis();
        ConcurrentList<byte[]> auctionTags2 = auctions
            .parallelStream()
            .map(SkyBlockAuction::getItemNbt)
            .filter(Objects::nonNull)
            .map(NbtContent::getData)
            .collect(Concurrent.toList());
        long end2 = System.currentTimeMillis();
        System.out.println("Collecting byte arrays took: " + (end2 - start2) + "ms");*/

        /*Benchmark auctionTags3 = SimplifiedApi.benchmark(() -> auctions.parallelStream()
            .map(SkyBlockAuction::getItemNbt)
            .filter(Objects::nonNull)
            .map(NbtContent::getNbtData)
            .collect(Concurrent.toList())
        );
        System.out.println("NBT parallel test took: " + auctionTags3.getDuration() + "ms");*/
    }

}
