package org.noise_planet.noisemodelling.pathfinder;

import org.locationtech.jts.geom.Coordinate;
import org.noise_planet.noisemodelling.pathfinder.path.BridgeRelationship;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Bridge;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;

/**
 * Analyzer for bridge-related geometric calculations in noise propagation.
 * 
 * Single responsibility: analyze bridge geometry and relationships to determine
 * bridge interactions with sound sources (e.g., selecting bridges for mirror sources).
 * This class must not perform sampling, elevation conversion, or modify scene state.
 */
public final class BridgeAnalyzer {
    
    private BridgeAnalyzer() {
        // Utility class, prevent instantiation
    }

    /**
     * Select the bridge that should create a MIRROR_SOURCE for the given source point.
     * 
     * Selection criteria:
     * - Source must be within bridge footprint (2D projection)
     * - Bridge bottom must be above source Z elevation
     * - Among multiple qualifying bridges, select the one with minimum bridge bottom height
     * - Do not select bridge if source type is IMAGINARY_SOURCE_UNDER_BRIDGE
     * - Do not select the bridge the source is already on
     * 
     * @param sourceCoord Source coordinate with absolute elevation
     * @param sourceZElevation Source absolute Z elevation
     * @param sourceProperty Source bridge relationship
     * @param profileBuilder Profile builder containing bridge data
     * @return Selected bridge for mirror source creation, or null if no qualifying bridge found
     */
    public static Bridge selectBridgeForMirrorSource(
            Coordinate sourceCoord,
            double sourceZElevation,
            BridgeRelationship sourceProperty,
            ProfileBuilder profileBuilder) {
        
        if (sourceProperty == null || profileBuilder == null) {
            return null;
        }
        
        // Do not create MIRROR_SOURCE for IMAGINARY_SOURCE_UNDER_BRIDGE
        if (sourceProperty.getRelationType() == BridgeRelationship.RelationType.IMAGINARY_SOURCE_UNDER_BRIDGE) {
            return null;
        }

        // Do not create MIRROR_SOURCE for another MIRROR_SOURCE
        if (sourceProperty.getRelationType() == BridgeRelationship.RelationType.MIRROR_SOURCE) {
            return null;
        }
        
        // Find bridge with minimum bridge bottom above the source
        Bridge selectedBridge = null;
        double minBridgeBottom = Double.POSITIVE_INFINITY;
        
        int bridgeCount = profileBuilder.getBridgeCount();
        for (int i = 0; i < bridgeCount; i++) {
            Bridge bridge = profileBuilder.getBridge(i);
            if (bridge == null) {
                continue;
            }
            
            // Skip if this is the bridge the source is on
            if (sourceProperty.getRelationType() == BridgeRelationship.RelationType.ACTUAL_SOURCE_ON_BRIDGE &&
                sourceProperty.getBridgePkOn() == bridge.getPrimaryKey()) {
                continue;
            }
            
            // Check if source is within bridge footprint
            if (!bridge.isPointWithinBridgeFootprint(sourceCoord)) {
                continue;
            }
            
            // Get bridge bottom elevation
            double deckHeight = bridge.getDeckHeightAtPoint(sourceCoord);
            double deckThickness = bridge.getDeckThicknessAtPoint(sourceCoord);
            if (Double.isNaN(deckHeight) || Double.isNaN(deckThickness)) {
                continue;
            }
            
            double bridgeBottom = deckHeight - deckThickness;
            
            // Bridge bottom must be above source
            if (bridgeBottom <= sourceZElevation) {
                continue;
            }
            
            // Select minimum bridge bottom
            if (bridgeBottom < minBridgeBottom) {
                minBridgeBottom = bridgeBottom;
                selectedBridge = bridge;
            }
        }
        
        return selectedBridge;
    }
}
