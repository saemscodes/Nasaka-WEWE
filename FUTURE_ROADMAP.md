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


A true "middle ground" for the next evolution of Nasaka WEWE would be a Zonal Hybrid Mesh.

This approach attempts to steal Bitchat’s "Metadata Blinding" and "Asynchronous Flooding" without suffering its battery-killing global overhead, all while doubling down on Nasaka’s "Offline Resilience."

Here is how that architecture would look:

1. Zonal Swarms (Regional Flooding)
Instead of Bitchat's global flood (which is too heavy) or Nasaka’s direct P2P (which leaks metadata), the app could implement Zonal Swarms.

The Idea: In a specific hotspot or neighborhood mesh, every device periodically broadcasts a "Pulse" containing encrypted blobs for everyone in that local area.
The Benefit: Within a 1km radius (Bluetooth/Wi-Fi range), it becomes impossible for an observer to tell who is talking to whom. You get Bitchat-level metadata anonymity, but restricted to a small "Zone" so the battery isn't drained by global traffic.
2. "Blind" Storage Nodes (Distributed Asynchronicity)
Nasaka currently requires a 1:1 "Mailbox" phone. We can steal the "Floating Message" concept from Bitchat to create Blind Buffers.

The Idea: Volunteers with fixed internet or high-range radios (like CEKA offices) act as "Swarm Anchors." They don't know who has an account, but they store every encrypted blob they hear for 48 hours.
The Benefit: If Alice sends a message while Bob is offline, she "floods" it to the local Zonal Swarm. The Swarm Anchor catches it. When Bob walks into range an hour later, his phone syncs with the Anchor and pulls his message. It’s asynchronous like Bitchat, but localized like Nasaka.
3. Proof-of-Work Gated "Public Pulses"
One of Bitchat's best features is the "Chan" (public anonymous broadcasting).

The Idea: Nasaka WEWE could offer a "Broadcast" tab that is permissionless. To post an alert (e.g., "Roadblock at Ngong Road"), your phone must perform a 5-second "Proof of Work" (CPU calculation).
The Benefit: You don't need to be contacts with someone to hear their alert. It allows for a community-wide "Panic Pulse" that any Nasaka user in the area will receive, while the PoW ensures that a bad actor can't flood the community with fake alerts.
4. Adaptive Transport (The All-Rounder)
This is the "brain" that manages the landing.

The Idea: The app monitors its environment.
Abundance Mode (Wi-Fi/Charging): Acts like Bitchat. It participates in flooding, helps store blobs for others, and performs heavy metadata-blinding (sending dummy traffic).
Scarcity Mode (Battery Low/Mobile Data): Acts like Nasaka/Briar. It shuts down the swarm, stops flooding, and only does direct 1:1 encrypted P2P connections to save every drop of juice.
The Benefit: The app is "Elite" when it can afford to be, and "Minimalist" when it must survive.
5. Metadata Blinding (The "Chaff" Strategy)
Briar/Nasaka's biggest vulnerability is that the timing of packets can reveal a conversation.

The Idea: Steal Bitchat's "Constant Stream" idea but make it "Micro." Every minute, the app sends a tiny, encrypted packet of "chaff" (garbage data) to a random contact.
The Benefit: If the app is always sending a small amount of traffic, a government monitor can't tell the difference between a real "I'm heading to the protest" message and a "Chaff" packet. You hide the signal in the noise.
Summary: The Landing
The "Better All-Rounded" app isn't just a messenger; it’s a Local Information Swarm.

It uses Nasaka’s P2P engine for secure, long-form intimate chats.
It uses a Bitchat-style Flood layer for local news, anonymous alerts, and metadata protection.
It uses Distributed Anchors to make sure messages arrive even if you never meet your contact face-to-face.
By combining these, Nasaka WEWE moves from a "secure phone-to-phone tool" to a persistent, invisible community nervous system.