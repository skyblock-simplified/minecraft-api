package dev.sbs.minecraftapi.client.sbs.response;

import dev.simplified.collection.ConcurrentMap;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class SkyBlockEmojiData {

    private final SkyBlockItems items;
    private final SkyBlockEmojis emojis;
    private final SkyBlockImages images;

    public ConcurrentMap<String, ConcurrentMap<Boolean, SkyBlockEmojis.Emoji>> getEmojis() {
        return this.emojis.getItems();
    }

    public ConcurrentMap<String, SkyBlockImages.Image> getImages() {
        return this.images.getItems();
    }

    public ConcurrentMap<String, String> getItems() {
        return this.items.getItems();
    }


    public Optional<SkyBlockEmojis.Emoji> getEmoji(@NotNull String itemId) {
        return this.getEmoji(itemId, false);
    }

    public Optional<SkyBlockEmojis.Emoji> getEmoji(@NotNull String itemId, boolean enchanted) {
        return this.items.getItemId(itemId)
            .flatMap(id -> this.emojis.getEmoji(id, enchanted))
            .map(emoji -> new SkyBlockEmojis.Emoji(itemId, emoji));
    }

    public Optional<SkyBlockEmojis.Emoji> getPetEmoji(@NotNull String petId) {
        return this.getEmoji("PET_" + petId);
    }

    public Optional<SkyBlockEmojis.Emoji> getRuneEmoji(@NotNull String runeId) {
        return this.getEmoji("RUNE_" + runeId);
    }
}
