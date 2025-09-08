package dev.sbs.minecraftapi.skyblock.island.data.pet;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.stream.StreamUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class PetData {

    private final static @NotNull ConcurrentList<Integer> magicFindPetScore = Concurrent.newList(10, 25, 50, 75, 100, 130, 175, 225, 275, 325, 375, 450, 500);
    private final @NotNull ConcurrentList<PetEntry> pets = Concurrent.newList();
    private final @NotNull AutoPet autopet = new AutoPet();

    public @NotNull Optional<PetEntry> getActivePet() {
        return this.getPets()
            .stream()
            .filter(PetEntry::isActive)
            .findFirst();
    }

    public int getPetScore() {
        return this.getPets()
            .sorted(PetEntry::getRarity)
            .stream()
            .filter(StreamUtil.distinctByKey(PetEntry::getType))
            .mapToInt(PetEntry::getScore)
            .sum();
    }

    public int getPetScoreMagicFind() {
        return magicFindPetScore.matchAll(breakpoint -> breakpoint <= this.getPetScore())
            .reduce((m1, m2) -> m2)
            .orElse(0);
    }

}
