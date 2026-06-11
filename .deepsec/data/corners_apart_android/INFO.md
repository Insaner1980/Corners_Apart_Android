# Corners Apart Android security context

Corners Apart is a local-first Android board game. Local persistence uses DataStore and kotlinx.serialization JSON for saved games, profiles, and settings. Room and remote avatar services are intentionally out of scope for v1.

Nearby multiplayer must stay on Google Play services Nearby Connections. Do not add raw Bluetooth, Wi-Fi Direct, Wi-Fi Aware, or direct socket fallback paths for v1.

Important boundaries:

- Android manifest permissions must stay tied to Nearby Connections runtime requirements.
- The launcher activity is the only expected exported component.
- `HostGameCoordinator` owns host-authoritative Nearby move validation.
- `NearbySession` and `GameProtocol` are the message boundary for multiplayer state.
- DataStore repositories own persisted profile, settings, and saved-game data.
- Logs must not disclose Nearby endpoints, payloads, profile data, saved games, board state, or move history.
