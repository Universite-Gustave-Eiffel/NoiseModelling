package org.noise_planet.noisemodelling.pathfinder.path;

import java.util.Objects;

/**
 * Container for properties describing how a source relates to a bridge.
 * <p>
 * This class is used to mark a source as being located on a bridge, under a
 * bridge (imaginary), a mirror source, or not related to any bridge. It also
 * carries the bridge primary key when relevant.
 */
public class BridgeRelationship {
    /**
     * Enumerates how a source is related to a bridge.
     */
    public enum RelationType {
        /** Source is not related to any bridge. */
        SOURCE_NOT_RELATED_TO_BRIDGE,
        /** The source is an actual physical source located on a bridge. */
        ACTUAL_SOURCE_ON_BRIDGE,
        /** An imaginary (virtual) source located under a bridge. */
        IMAGINARY_SOURCE_UNDER_BRIDGE,
        /** A mirror source used for reflection modelling. */
        MIRROR_SOURCE
    }

    /** Relation type of the source to a bridge. Defaults to not related. */
    private RelationType relationType = RelationType.SOURCE_NOT_RELATED_TO_BRIDGE;

    /** Primary key of the bridge related to this source (-1 if none). */
    private long bridgePkOn = -1L;
    /** Primary key of the bridge related to this source (-1 if none). */
    private long bridgePkAbove = -1L;

    /**
     * Create an empty BridgeRelationship instance with default values.
     */
    public BridgeRelationship() {}

    /**
     * Create BridgeRelationship with explicit type and bridge identifier.
     *
     * @param relationType relation of the source to a bridge
        * @param bridgePkOn primary key of the bridge supporting the source (use -1 if not applicable)
        * @param bridgePkAbove primary key of the bridge above the source (use -1 if not applicable)
     */
    public BridgeRelationship(RelationType relationType, long bridgePkOn, long bridgePkAbove) {
        this.relationType = relationType;
        this.bridgePkOn = bridgePkOn;
        this.bridgePkAbove = bridgePkAbove;
    }

    /**
     * Get the bridge primary key associated with these properties.
     *
     * @return bridge primary key (or -1 if not set)
     */
    public long getBridgePkOn() {
        return bridgePkOn;
    }

    public long getBridgePkAbove() {
        return bridgePkAbove;
    }

    /**
     * Get the source type indicating relation to a bridge.
     *
     * @return RelationType enum value describing the relation
     */
    public RelationType getRelationType() {
        return relationType;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        BridgeRelationship that = (BridgeRelationship) o;
        return bridgePkOn == that.getBridgePkOn() && 
               bridgePkAbove == that.getBridgePkAbove() && 
               (relationType == that.getRelationType() || 
                (relationType != null && relationType.equals(that.getRelationType())));
    }

    
    @Override
    public int hashCode() {
        return Objects.hash(bridgePkOn, bridgePkAbove, relationType);
    }

}
