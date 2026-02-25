package org.briarproject.bramble.plugin.bluetooth;

import org.briarproject.bramble.api.nullsafety.NotNullByDefault;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import javax.annotation.concurrent.ThreadSafe;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

/**
 * Nasaka WEWE Mesh Router — Layer 3 (Routing Engine)
 *
 * Implements store-and-forward relay routing with:
 * - TTL enforcement (decrement per hop, drop at 0)
 * - Deduplication via SHA-256 packet hashing
 * - Encounter score tracking with exponential decay
 * - Persistent packet queue for store-and-forward
 */
@ThreadSafe
@NotNullByDefault
public class MeshRouter {

    private static final Logger LOG = Logger.getLogger(MeshRouter.class.getName());

    /** Maximum TTL for any packet in the mesh */
    public static final int MAX_TTL = 10;

    /** Default TTL for new packets */
    public static final int DEFAULT_TTL = 5;

    /** Decay factor for encounter scores (0.9 = 10% decay per interval) */
    private static final double ENCOUNTER_DECAY = 0.9;

    /** Maximum packets stored for forward delivery */
    private static final int MAX_STORED_PACKETS = 500;

    // Thread-safe collections for routing state
    private final Map<String, Long> seenPackets = new ConcurrentHashMap<>();
    private final Map<String, Double> encounterScores = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<MeshPacket> storedPackets = new CopyOnWriteArrayList<>();

    /**
     * Represents a single packet in the Nasaka mesh network.
     */
    public static class MeshPacket {
        public final String sourceId;
        public final String destinationId;
        public final byte[] payload;
        public int ttl;
        public final long timestamp;
        public final String packetHash;

        public MeshPacket(String sourceId, String destinationId, byte[] payload, int ttl) {
            this.sourceId = sourceId;
            this.destinationId = destinationId;
            this.payload = payload;
            this.ttl = ttl;
            this.timestamp = System.currentTimeMillis();
            this.packetHash = computeHash(sourceId, destinationId, payload, timestamp);
        }

        private static String computeHash(String src, String dst, byte[] payload, long ts) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(src.getBytes());
                digest.update(dst.getBytes());
                digest.update(payload);
                digest.update(ByteBuffer.allocate(8).putLong(ts).array());
                byte[] hash = digest.digest();
                StringBuilder sb = new StringBuilder();
                for (byte b : hash) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-256 not available", e);
            }
        }
    }

    /**
     * Process an incoming packet. Returns true if the packet should be
     * forwarded to locally connected peers, false if it should be dropped.
     *
     * Applies:
     * 1. Deduplication — reject packets we've already seen
     * 2. TTL enforcement — drop packets with TTL <= 0
     * 3. TTL decrement — reduce TTL for next hop
     * 4. Store-and-forward — queue for later delivery if destination is offline
     */
    public boolean processIncomingPacket(MeshPacket packet) {
        // 1. Deduplication check
        if (seenPackets.containsKey(packet.packetHash)) {
            if (LOG.isLoggable(INFO)) {
                LOG.info("Duplicate packet dropped: " + packet.packetHash.substring(0, 8));
            }
            return false;
        }

        // 2. TTL enforcement
        if (packet.ttl <= 0) {
            if (LOG.isLoggable(WARNING)) {
                LOG.warning("Packet TTL expired, dropping: " + packet.packetHash.substring(0, 8));
            }
            return false;
        }

        // Mark as seen
        seenPackets.put(packet.packetHash, System.currentTimeMillis());

        // 3. Decrement TTL for next hop
        packet.ttl--;

        // 4. Store for forward delivery
        storeForForward(packet);

        if (LOG.isLoggable(INFO)) {
            LOG.info("Packet accepted for relay: " + packet.packetHash.substring(0, 8)
                    + " TTL=" + packet.ttl + " src=" + packet.sourceId);
        }

        return true;
    }

    /**
     * Create a new packet originating from this device.
     */
    public MeshPacket createPacket(String sourceId, String destinationId, byte[] payload) {
        MeshPacket packet = new MeshPacket(sourceId, destinationId, payload, DEFAULT_TTL);
        seenPackets.put(packet.packetHash, System.currentTimeMillis());
        storeForForward(packet);
        return packet;
    }

    /**
     * Store a packet for delayed forwarding (store-and-forward).
     * Evicts oldest packets when the buffer is full.
     */
    private void storeForForward(MeshPacket packet) {
        if (storedPackets.size() >= MAX_STORED_PACKETS) {
            // Evict the oldest packet
            storedPackets.remove(0);
        }
        storedPackets.add(packet);
    }

    /**
     * Retrieve all stored packets destined for a specific peer.
     * Removes delivered packets from the store.
     */
    public MeshPacket[] getPacketsForPeer(String peerId) {
        CopyOnWriteArrayList<MeshPacket> results = new CopyOnWriteArrayList<>();
        for (MeshPacket p : storedPackets) {
            if (p.destinationId.equals(peerId)) {
                results.add(p);
            }
        }
        storedPackets.removeAll(results);
        return results.toArray(new MeshPacket[0]);
    }

    /**
     * Retrieve all stored packets that still have TTL > 0 for relay.
     */
    public MeshPacket[] getRelayablePackets() {
        CopyOnWriteArrayList<MeshPacket> results = new CopyOnWriteArrayList<>();
        for (MeshPacket p : storedPackets) {
            if (p.ttl > 0) {
                results.add(p);
            }
        }
        return results.toArray(new MeshPacket[0]);
    }

    /**
     * Record an encounter with a peer. Encounter scores determine
     * routing priority — higher scores mean more frequent encounters.
     */
    public void recordEncounter(String peerId) {
        Double current = encounterScores.get(peerId);
        double newScore = (current != null ? current : 0.0) + 1.0;
        encounterScores.put(peerId, newScore);
        if (LOG.isLoggable(INFO)) {
            LOG.info("Encounter recorded for " + peerId + " score=" + newScore);
        }
    }

    /**
     * Get the encounter score for a peer.
     */
    public double getEncounterScore(String peerId) {
        Double score = encounterScores.get(peerId);
        return score != null ? score : 0.0;
    }

    /**
     * Apply exponential decay to all encounter scores.
     * Should be called periodically (e.g. every 10 minutes).
     */
    public void decayEncounterScores() {
        for (Map.Entry<String, Double> entry : encounterScores.entrySet()) {
            double decayed = entry.getValue() * ENCOUNTER_DECAY;
            if (decayed < 0.01) {
                encounterScores.remove(entry.getKey());
            } else {
                encounterScores.put(entry.getKey(), decayed);
            }
        }
    }

    /**
     * Purge stale seen-packet entries older than the given age (in ms).
     * Prevents the dedup cache from growing unbounded.
     */
    public void purgeStaleEntries(long maxAgeMs) {
        long cutoff = System.currentTimeMillis() - maxAgeMs;
        seenPackets.entrySet().removeIf(e -> e.getValue() < cutoff);
    }

    /**
     * Get the number of packets currently stored for forwarding.
     */
    public int getStoredPacketCount() {
        return storedPackets.size();
    }

    /**
     * Get the number of unique peers encountered.
     */
    public int getEncounteredPeerCount() {
        return encounterScores.size();
    }
}
