package com.finnvek.cornersapart.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.cornersapart.model.BoardSnapshot
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.model.PieceTransforms
import com.finnvek.cornersapart.ui.theme.CornersApartTheme
import com.finnvek.cornersapart.viewmodel.GameUiState
import com.finnvek.cornersapart.viewmodel.PiecePanelItem
import com.finnvek.cornersapart.viewmodel.PlayerUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun gameScreenShowsBoardAndAccessibleControls() {
        composeRule.setContent {
            CornersApartTheme {
                GameScreenContent(
                    state = testUiState(),
                    onModeSelected = {},
                    onSelectPiece = {},
                    onRotateCounterClockwise = {},
                    onRotateClockwise = {},
                    onFlip = {},
                    onPass = {},
                    onPlaceCell = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Game board").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Rotate counterclockwise").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Rotate clockwise").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Flip selected piece").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pass turn").assertIsDisplayed()
    }

    @Test
    fun historyStatsDialogShowsHistoryAndStatsTabs() {
        composeRule.setContent {
            CornersApartTheme {
                GameScreenContent(
                    state = testUiState(),
                    onModeSelected = {},
                    onSelectPiece = {},
                    onRotateCounterClockwise = {},
                    onRotateClockwise = {},
                    onFlip = {},
                    onPass = {},
                    onPlaceCell = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("History & stats").performClick()

        composeRule.onNodeWithText("History").assertIsDisplayed()
        composeRule.onNodeWithText("Stats").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Total games").assertIsDisplayed()
    }

    @Test
    fun settingsDialogShowsPolishToggles() {
        composeRule.setContent {
            CornersApartTheme {
                GameScreenContent(
                    state = testUiState(),
                    onModeSelected = {},
                    onSelectPiece = {},
                    onRotateCounterClockwise = {},
                    onRotateClockwise = {},
                    onFlip = {},
                    onPass = {},
                    onPlaceCell = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("Settings").performClick()

        composeRule.onNodeWithText("Sound").assertIsDisplayed()
        composeRule.onNodeWithText("Haptics").assertIsDisplayed()
        composeRule.onNodeWithText("Reduced motion").assertIsDisplayed()
    }

    @Test
    fun helpDialogShowsOriginalCornersApartRules() {
        composeRule.setContent {
            CornersApartTheme {
                GameScreenContent(
                    state = testUiState(),
                    onModeSelected = {},
                    onSelectPiece = {},
                    onRotateCounterClockwise = {},
                    onRotateClockwise = {},
                    onFlip = {},
                    onPass = {},
                    onPlaceCell = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("Help").performClick()

        composeRule.onNodeWithText("Goal").assertIsDisplayed()
        composeRule.onNodeWithText("Start in your corner").assertIsDisplayed()
        composeRule.onNodeWithText("Corner contact, no edge contact").assertIsDisplayed()
    }

    @Test
    fun gameOverDialogShowsScoreBreakdown() {
        composeRule.setContent {
            CornersApartTheme {
                GameScreenContent(
                    state = testUiState(isGameOver = true),
                    onModeSelected = {},
                    onSelectPiece = {},
                    onRotateCounterClockwise = {},
                    onRotateClockwise = {},
                    onFlip = {},
                    onPass = {},
                    onPlaceCell = { _, _ -> },
                )
            }
        }

        composeRule.onAllNodesWithText("Game over")[0].assertIsDisplayed()
        composeRule.onNodeWithText("Placed cells").assertIsDisplayed()
        composeRule.onNodeWithText("Bonus tiles").assertIsDisplayed()
        composeRule.onNodeWithText("Completion bonus").assertIsDisplayed()
        composeRule.onNodeWithText("Play again").assertIsDisplayed()
    }

    private fun testUiState(isGameOver: Boolean = false): GameUiState {
        val selectedPiece = PieceCatalog.require(PieceCatalog.THREE_BEND_ID)
        return GameUiState(
            gameMode = GameMode.FOUR_PLAYER,
            board = BoardSnapshot.empty(GameConstants.STANDARD_BOARD_SIZE),
            bonusTiles = emptyList(),
            players =
                GameConstants.PLAYER_NAMES.mapIndexed { index, name ->
                    PlayerUiState(
                        index = index,
                        name = name,
                        colorIndex = index,
                        ownerIndex = index,
                        startRow = 0,
                        startCol = 0,
                        totalScore = index,
                        placedCellPoints = index,
                        bonusTilePoints = index * 3,
                        completionBonus = if (index == 0) 10 else 0,
                        claimedBonusTiles = index,
                        piecesPlaced = index,
                        piecesRemaining = GameConstants.PIECE_COUNT - index,
                        hasPassed = false,
                        isCurrentTurn = index == 0,
                        isComputerControlled = false,
                    )
                },
            currentPlayerIndex = 0,
            selectedPieceId = selectedPiece.id,
            selectedOrientationIndex = 0,
            selectedCells = PieceTransforms.getAllOrientations(selectedPiece).first(),
            pieces =
                PieceCatalog.all.map { piece ->
                    PiecePanelItem(
                        piece = piece,
                        isSelected = piece.id == selectedPiece.id,
                        isUsed = false,
                    )
                },
            isGameOver = isGameOver,
        )
    }
}
