package com.finnvek.cornersapart.opponents

/**
 * Nimetty konevastustaja: kiinteä tyylin ja vaikeustason yhdistelmä, jolla on
 * oma nimi ja väriperhe. Nimet ovat koodivakioita samaan tapaan kuin
 * GameConstants.PLAYER_NAMES; muut käyttöliittymätekstit ovat resursseissa.
 */
data class OpponentCharacter(
    val id: String,
    val name: String,
    val style: OpponentStyle,
    val difficulty: OpponentDifficulty,
    val colorIndex: Int,
) {
    val tier: Int
        get() = difficulty.ordinal + 1
}

/**
 * Rivals-tikapuut: järjestetty lista vastustajia, joista seuraava aukeaa
 * voittamalla edellisen vähintään kerran. Ensimmäinen on aina auki.
 */
object OpponentRoster {
    val all: List<OpponentCharacter> =
        listOf(
            character("jelly", "Jelly", OpponentStyle.EXPANSIONIST, OpponentDifficulty.BEGINNER, colorIndex = 0),
            character("pip", "Pip", OpponentStyle.OPPORTUNIST, OpponentDifficulty.BEGINNER, colorIndex = 1),
            character("sprout", "Sprout", OpponentStyle.EXPANSIONIST, OpponentDifficulty.EASY, colorIndex = 3),
            character("coco", "Coco", OpponentStyle.BLOCKER, OpponentDifficulty.EASY, colorIndex = 0),
            character("dash", "Dash", OpponentStyle.OPPORTUNIST, OpponentDifficulty.MEDIUM, colorIndex = 2),
            character("fig", "Fig", OpponentStyle.BLOCKER, OpponentDifficulty.MEDIUM, colorIndex = 3),
            character("blaze", "Blaze", OpponentStyle.EXPANSIONIST, OpponentDifficulty.HARD, colorIndex = 1),
            character("luna", "Luna", OpponentStyle.OPPORTUNIST, OpponentDifficulty.HARD, colorIndex = 2),
            character("onyx", "Onyx", OpponentStyle.BLOCKER, OpponentDifficulty.EXPERT, colorIndex = 0),
            character("nova", "Nova", OpponentStyle.EXPANSIONIST, OpponentDifficulty.EXPERT, colorIndex = 2),
            character("vex", "Vex", OpponentStyle.OPPORTUNIST, OpponentDifficulty.MASTER, colorIndex = 1),
            character("sol", "Sol", OpponentStyle.BLOCKER, OpponentDifficulty.MASTER, colorIndex = 3),
        )

    fun forId(id: String): OpponentCharacter? = all.firstOrNull { character -> character.id == id }

    fun isUnlocked(
        id: String,
        wins: Map<String, Int>,
    ): Boolean {
        val index = all.indexOfFirst { character -> character.id == id }
        if (index < 0) return false
        if (index == 0) return true
        return (wins[all[index - 1].id] ?: 0) > 0
    }

    /** Seuraava vielä voittamaton avoin vastustaja — galleriassa korostettu haastaja. */
    fun nextChallenger(wins: Map<String, Int>): OpponentCharacter? =
        all.firstOrNull { character ->
            (wins[character.id] ?: 0) == 0 && isUnlocked(character.id, wins)
        }

    /** Vastustaja, joka aukeaa kun [id] voitetaan ensimmäistä kertaa. */
    fun unlockedByFirstWinOf(id: String): OpponentCharacter? {
        val index = all.indexOfFirst { character -> character.id == id }
        if (index < 0) return null
        return all.getOrNull(index + 1)
    }

    private fun character(
        id: String,
        name: String,
        style: OpponentStyle,
        difficulty: OpponentDifficulty,
        colorIndex: Int,
    ): OpponentCharacter =
        OpponentCharacter(
            id = id,
            name = name,
            style = style,
            difficulty = difficulty,
            colorIndex = colorIndex,
        )
}
