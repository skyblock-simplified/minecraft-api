package dev.sbs.minecraftapi.builder.generator.exception;

import dev.sbs.minecraftapi.exception.MinecraftException;
import org.intellij.lang.annotations.PrintFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GeneratorException extends MinecraftException {

    public GeneratorException(@NotNull Throwable cause) {
        super(cause);
    }

    public GeneratorException(@NotNull String message) {
        super(message);
    }

    public GeneratorException(@NotNull Throwable cause, @NotNull String message) {
        super(cause, message);
    }

    public GeneratorException(@NotNull @PrintFormat String message, @Nullable Object... args) {
        super(message, args);
    }

    public GeneratorException(@NotNull Throwable cause, @NotNull @PrintFormat String message, @Nullable Object... args) {
        super(cause, message, args);
    }

}
