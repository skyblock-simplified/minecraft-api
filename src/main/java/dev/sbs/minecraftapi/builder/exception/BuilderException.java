package dev.sbs.minecraftapi.builder.exception;

import dev.sbs.minecraftapi.exception.MinecraftException;
import org.intellij.lang.annotations.PrintFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BuilderException extends MinecraftException {

    public BuilderException(@NotNull Throwable cause) {
        super(cause);
    }

    public BuilderException(@NotNull String message) {
        super(message);
    }

    public BuilderException(@NotNull Throwable cause, @NotNull String message) {
        super(cause, message);
    }

    public BuilderException(@NotNull @PrintFormat String message, @Nullable Object... args) {
        super(message, args);
    }

    public BuilderException(@NotNull Throwable cause, @NotNull @PrintFormat String message, @Nullable Object... args) {
        super(cause, message, args);
    }

}
