package dev.sbs.minecraftapi.model;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.JpaModel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Entity
@Table(name = "item_categories")
public class ItemCategory implements JpaModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    @Enumerated(EnumType.STRING)
    private @NotNull Item.Type type = Item.Type.OTHER;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ItemCategory that = (ItemCategory) o;

        return new EqualsBuilder()
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getType(), that.getType())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getType())
            .build();
    }

}