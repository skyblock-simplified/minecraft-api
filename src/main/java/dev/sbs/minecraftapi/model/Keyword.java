package dev.sbs.minecraftapi.model;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Optional;

@Getter
@Entity
@Table(name = "keywords")
public class Keyword implements JpaModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull Optional<String> plural = Optional.empty();
    private @NotNull Optional<String> symbol = Optional.empty();
    @Enumerated(EnumType.STRING)
    private @NotNull ChatFormat format = ChatFormat.GREEN;

    public boolean hasPlural() {
        return this.getPlural().isPresent();
    }

    public boolean hasSymbol() {
        return this.getSymbol().isPresent();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Keyword that = (Keyword) o;

        return new EqualsBuilder()
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getPlural(), that.getPlural())
            .append(this.getSymbol(), that.getSymbol())
            .append(this.getFormat(), that.getFormat())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getPlural())
            .append(this.getSymbol())
            .append(this.getFormat())
            .build();
    }

}