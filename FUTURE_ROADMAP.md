# Nasaka WEWE: Hybrid Architecture Roadmap
## The "Zonal Hybrid Mesh" Technical Blueprint

This roadmap outlines the evolution of Nasaka WEWE from a point-to-point (P2P) synchronous messenger (Briar-based) to an **Anonymized Distributed Swarm**, stealing the best elements of **Bitchat/Bitmessage** while doubling down on **Nasaka's Offline Resilience**.

---

## 1. Zonal Swarms (Regional Metadata Blinding)

### The Objective
Eliminate packet-timing and address-linkage vulnerabilities within local mesh areas without the battery cost of a global flood.

### Implementation Details
- **Geographic Sharding**: Instead of a global stream, the network is sharded into "Zones" based on Bluetooth/Wi-Fi hop distance.
- **Micro-Flooding**: All messages within a Zone are broadcast to every node in that Zone.
- **Reception Logic**: Every device attempts to "blindly" decrypt every packet. Only success results in a notification. To an observer, every user is receiving a constant stream of identical noise.
- **Traffic Shaping**: When the swarm is quiet, nodes generate "Chaff" (simulated encrypted traffic) to maintain a constant data volume. This makes it impossible for an adversary to detect when a real "high-value" message is actually being sent.

---

## 2. Distributed Blind storage nodes (The "Swarm Anchors")

### The Objective
Achieve Bitchat-style asynchronicity (messages waiting for you in the cloud/air) without compromising Nasaka’s "Zero Central Server" principle.

### Implementation Details
- **Swarm Anchors**: High-uptime nodes (CEKA community offices, volunteer desktops, or raspberry pi "dead-drops") act as Anchor points.
- **Blind Buffering**: Anchors store all encrypted blobs they hear for a rolling 48-72 hour window. They have no knowledge of IDs or Keys; they simply cache bits.
- **Pull-on-Wake**: When a Nasaka user (who has been offline) walks into range of an Anchor, their app "scans" the Anchor’s buffer. If a blooming-filter matches a local key, the app pulls the full blob.
- **Asynchronous Handshake**: Allows Alice to leave a message for Bob even if Bob won't be online for another two days. The "Network" becomes the buffer.

---

## 3. Proof-of-Work (PoW) Gated "Public Pulses"

### The Objective
Enable permissionless, anonymous citizen journalism and emergency alerts while preventing the spam/DDoS issues inherent in open Bitchat channels.

### Implementation Details
- **The Broadcast Tab**: A dedicated UI section for "Pulses" (Public Channels). 
- **Computational Cost**: Unlike private 1:1 chats, sending a "Pulse" requires the device to calculate a CPU-intensive Proof-of-Work (hash-completion). 
- **Spam Resistance**: This cost is negligible for a citizen reporting a protest (e.g. 5 seconds of CPU), but prohibitive for a bot looking to flood the network with 100,000 spam links.
- **Trust Tiers**: Optional "CEKA Verified" badges. If a user has authenticated online via Supabase, their PoW Pulse can be digitally signed with a community key, marking it as "Trusted Source" in the local UI.

---

## 4. Adaptive Resource Management (Dynamic Scaling)

### The Objective
Maintain 100% mission functionality (messaging) in extreme battery/data environments while offering 100% security features in abundance environments.

### Implementation Details
- **Resource Aware Profiles**:
    - **ELITE MODE (Plugged in/Unlimited Data/Wi-Fi)**: Max security. Participates in Swarm Anchoring, generates high-volume Chaff, performs deep metadata blinding for the whole zone.
    - **RESILIENT MODE (Battery > 30%/Mobile Data)**: Standard security. Syncs via Tor, participates in private 1:1 P2P, pulls from Anchors but doesn't serve others.
    - **BLACKOUT MODE (Battery < 15%/No Tor)**: Minimum vital signs. Shuts down background flooding. Only wakes for Bluetooth/Wi-Fi handshakes with known contacts. Minimal UI.
- **Transparent Toggle**: User can manually "Go Dark" to enter Blackout Mode even when battery is high if they feel they are being actively tracked via radio signatures.

---

## 5. Integration with CEKA Supabase Bridge

### The Objective
Link the high-trust "Member" status with the low-trust "Mesh" environment.

### Implementation Details
- **Identity Pinning**: Optionally lock a Nasaka WEWE identity to a CEKA membership via an encrypted cross-reference stored in the local DB.
- **Verified Reputation**: When a verified member posts a Public Pulse, it is prioritized in neighbors' feeds.
- **Sync Recovery**: If a device is lost during a blackout, the local "Identity Fragment" can be optionally backed up (encrypted) to the CEKA Supabase storage, allowing for identity recovery once internet returns.

---

## 6. Technical Stack Requirements
- **Encryption**: Double Ratchet (Signal Protocol) for 1:1; Onion-routed broadcast for Pulses.
- **Discovery**: Bluetooth LE + Wi-Fi Direct (mDNS) with random UUID rotation to prevent MAC-address tracking.
- **Database**: SQLite (Encrypted) with Bloom Filters for rapid "blind" scanning of remote anchors.

---

> **Status**: PENDING IMPLEMENTATION
> **Goal**: Nasaka WEWE v2.0
> **Mission**: Absolute Anonymity. Absolute Resilience. Absolute Local Empowerment.
