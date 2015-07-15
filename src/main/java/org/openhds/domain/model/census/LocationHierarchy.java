package org.openhds.domain.model.census;

import org.openhds.domain.contract.AuditableExtIdEntity;
import org.openhds.domain.util.Description;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Description(description = "A node in treelike representation of the study area geography.")
@Entity
@Table(name = "locationhierarchy")
public class LocationHierarchy extends AuditableExtIdEntity implements Serializable {

    private static final long serialVersionUID = -5334850119671675888L;

    @Description(description = "Parent location's name.")
    @ManyToOne
    private LocationHierarchy parent;

    @Description(description = "Level of the location hierarchy.")
    @ManyToOne
    private LocationHierarchyLevel level;

    @NotNull(message = "location hierarchy name may not be null")
    @Description(description = "The name of this location hierarchy record.")
    private String name;

    @Override
    public String getExtId() {
        return extId;
    }

    @Override
    public void setExtId(String extId) {
        this.extId = extId;
    }

    public LocationHierarchy getParent() {
        return parent;
    }

    public void setParent(LocationHierarchy parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocationHierarchyLevel getLevel() {
        return level;
    }

    public void setLevel(LocationHierarchyLevel level) {
        this.level = level;
    }

    @Override
    public String toString() {
        return "LocationHierarchy{" +
                "parent=" + parent +
                ", name='" + name + '\'' +
                ", level=" + level +
                "} " + super.toString();
    }
}
