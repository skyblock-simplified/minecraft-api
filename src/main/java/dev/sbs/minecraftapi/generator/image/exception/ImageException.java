package dev.sbs.minecraftapi.generator.image.exception;

import dev.sbs.minecraftapi.exception.MinecraftException;
import org.intellij.lang.annotations.PrintFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ImageException extends MinecraftException {

    public ImageException(@NotNull Throwable cause) {
        super(cause);
    }

    public ImageException(@NotNull String message) {
        super(message);
    }

    public ImageException(@NotNull Throwable cause, @NotNull String message) {
        super(cause, message);
    }

    public ImageException(@NotNull @PrintFormat String message, @Nullable Object... args) {
        super(message, args);
    }

    public ImageException(@NotNull Throwable cause, @NotNull @PrintFormat String message, @Nullable Object... args) {
        super(cause, message, args);
    }

}
