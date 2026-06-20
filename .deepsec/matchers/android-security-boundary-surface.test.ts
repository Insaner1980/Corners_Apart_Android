import assert from "node:assert/strict";
import { test } from "node:test";
import { androidSecurityBoundarySurface } from "./android-security-boundary-surface.js";

test("flags Nearby payload trust boundaries in production multiplayer code", () => {
  const content = `
class NearbyConnectionsCoordinator {
  private suspend fun sendMessage(message: GameMessage) {
    val bytes = GameProtocol.encode(message).encodeToByteArray()
    facade.sendPayload(endpointId, bytes)
  }

  private suspend fun handleBytesPayload(bytes: ByteArray) {
    val message = GameProtocol.decode(bytes.decodeToString())
  }
}`;

  const matches = androidSecurityBoundarySurface.match(
    content,
    "app/src/main/java/com/finnvek/cornersapart/multiplayer/NearbyConnectionsCoordinator.kt",
  );

  assert.equal(matches.length, 2);
  assert.deepEqual(
    matches.map((match) => match.matchedPattern).sort(),
    ["Nearby inbound protocol decode boundary", "Nearby outbound payload send boundary"],
  );
});

test("flags JSON DataStore persistence boundaries in production data code", () => {
  const content = `
class JsonDataStoreSerializer<T> : Serializer<T> {
  override suspend fun readFrom(input: InputStream): T =
    json.decodeFromString(serializer, input.readBytes().decodeToString())

  override suspend fun writeTo(t: T, output: OutputStream) {
    output.write(json.encodeToString(serializer, t).encodeToByteArray())
  }
}`;

  const matches = androidSecurityBoundarySurface.match(
    content,
    "app/src/main/java/com/finnvek/cornersapart/data/JsonDataStoreSerializer.kt",
  );

  assert.equal(matches.length, 2);
  assert.deepEqual(
    matches.map((match) => match.matchedPattern).sort(),
    ["JSON DataStore read boundary", "JSON DataStore write boundary"],
  );
});

test("skips test files", () => {
  const matches = androidSecurityBoundarySurface.match(
    "GameProtocol.decode(payload)",
    "app/src/test/java/com/finnvek/cornersapart/multiplayer/NearbyConnectionsCoordinatorTest.kt",
  );

  assert.deepEqual(matches, []);
});
