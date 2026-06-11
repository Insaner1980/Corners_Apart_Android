package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.ScoreDelta
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.finnvek.cornersapart.model.GameConfig as ModelGameConfig

@Serializable
sealed interface GameMessage {
    @Serializable
    @SerialName("placeMove")
    data class PlaceMove(
        val move: Move,
    ) : GameMessage

    @Serializable
    @SerialName("moveAccepted")
    data class MoveAccepted(
        val move: Move,
        val state: GameState,
        val scoreDelta: ScoreDelta = ScoreDelta(),
    ) : GameMessage

    @Serializable
    @SerialName("moveRejected")
    data class MoveRejected(
        val move: Move,
        val reason: String,
    ) : GameMessage

    @Serializable
    @SerialName("pass")
    data class Pass(
        val playerIndex: Int,
    ) : GameMessage

    @Serializable
    @SerialName("fullSync")
    data class FullSync(
        val state: GameState,
    ) : GameMessage

    @Serializable
    @SerialName("playerJoined")
    data class PlayerJoined(
        val player: SessionPlayer,
    ) : GameMessage

    @Serializable
    @SerialName("playerLeft")
    data class PlayerLeft(
        val playerIndex: Int,
    ) : GameMessage

    @Serializable
    @SerialName("gameConfig")
    data class GameConfig(
        val config: ModelGameConfig,
    ) : GameMessage

    @Serializable
    @SerialName("ping")
    data object Ping : GameMessage

    @Serializable
    @SerialName("pong")
    data object Pong : GameMessage
}

object GameProtocol {
    private val json =
        Json {
            classDiscriminator = "type"
            encodeDefaults = true
            ignoreUnknownKeys = false
        }

    fun encode(message: GameMessage): String = json.encodeToString(GameMessage.serializer(), message)

    fun decode(payload: String): GameMessage = json.decodeFromString(GameMessage.serializer(), payload)
}
