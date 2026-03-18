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

@Getter
@Entity
@Table(name = "stat_categories")
public class StatCategory implements JpaModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    @Enumerated(EnumType.STRING)
    private @NotNull ChatFormat format = ChatFormat.WHITE;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        StatCategory that = (StatCategory) o;

        return new EqualsBuilder()
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getFormat(), that.getFormat())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getFormat())
            .build();
    }

}