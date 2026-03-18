package dev.sbs.minecraftapi.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Getter
@Entity
@Table(name = "bestiary_subcategories")
public class BestiarySubcategory implements JpaModel {

    @Id
    @Column(name = "id", nullable = false)
    private @NotNull String id = "";

    @Column(name = "name", nullable = false)
    private @NotNull String name = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false)
    private @NotNull ChatFormat format = ChatFormat.GREEN;

    @SerializedName("category")
    @Column(name = "category_id", nullable = false)
    private @NotNull String categoryId = "";

    @Column(name = "ordinal", nullable = false)
    private int ordinal = -1;

    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private transient @NotNull BestiaryCategory category;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        BestiarySubcategory that = (BestiarySubcategory) o;

        return new EqualsBuilder()
            .append(this.getOrdinal(), that.getOrdinal())
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getFormat(), that.getFormat())
            .append(this.getCategoryId(), that.getCategoryId())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getFormat())
            .append(this.getCategoryId())
            .append(this.getOrdinal())
            .build();
    }

}
