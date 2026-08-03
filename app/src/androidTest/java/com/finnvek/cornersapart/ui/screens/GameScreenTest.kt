package com.finnvek.cornersapart.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.cornersapart.model.BoardSnapshot
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.model.PieceTransforms
import com.finnvek.cornersapart.model.PlayerScore
import com.finnvek.cornersapart.model.ScoreBreakdown
import com.finnvek.cornersapart.multiplayer.ConnectionState
import com.finnvek.cornersapart.multiplayer.NearbyEndpointUiState
import com.finnvek.cornersapart.multiplayer.NearbyPendingConnection
import com.finnvek.cornersapart.multiplayer.NearbyUiState
import com.finnvek.cornersapart.multiplayer.SessionType
import com.finnvek.cornersapart.testing.ComposeTestActivity
import com.finnvek.cornersapart.ui.theme.CornersApartTheme
import com.finnvek.cornersapart.viewmodel.GameUiState
import com.finnvek.cornersapart.viewmodel.PiecePanelItem
import com.finnvek.cornersapart.viewmodel.PlayerUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestActivity>()

    @Test
    fun gameScreenShowsBoardAndAccessibleControls() {
        composeRule.setContent {
            CornersApartTheme {
                GameScreenContent(
                    state = testUiState(),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Game board").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Rotate counterclockwise").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Rotate clockwise").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Flip selected piece").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pass turn").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun localGameDoesNotShowNearbyConnectionStatus() {
        composeRule.setContent {
            CornersApartTheme {
                GameScreenContent(
                    state =
                        testUiState(
                            nearbyState = NearbyUiState(connectionState = ConnectionState.CONNECTED),
                        ),
                )
            }
        }

        composeRule.onAllNodesWithText("Status: Connected").assertCountEquals(0)
        composeRule.onNodeWithText("Nearby game").assertIsDisplayed()
    }

    @Test
    fun historyStatsDialogShowsHistoryAndStatsTabs() {
        composeRule.setContent {
            CornersApartTheme {
                var showHistoryStats by remember { mutableStateOf(false) }
                GameScreenContent(
                    state = testUiState(),
                    screenActions =
                        GameScreenActions(
                            onShowHistoryStats = { showHistoryStats = true },
                        ),
                    dialogState =
                        GameDialogState(
                            showHistoryStatsDialog = showHistoryStats,
                            onDismissHistoryStats = { showHistoryStats = false },
                        ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("History & stats").performScrollTo().performClick()

        composeRule.onNode(hasTextExactly("History") and hasClickAction()).assertIsDisplayed()
        composeRule.onNode(hasTextExactly("Stats") and hasClickAction()).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Total games").assertIsDisplayed()
    }

    @Test
    fun settingsDialogShowsActivePolishToggles() {
        composeRule.setContent {
            CornersApartTheme {
                GameScreenContent(
                    state = testUiState(),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Settings").performClick()

        composeRule.onNodeWithText("Sound").assertIsDisplayed()
        composeRule.onNodeWithText("Haptics").assertIsDisplayed()
        composeRule.onAllNodesWithText("Reduced motion").assertCountEquals(0)
    }

    @Test
    fun helpDialogShowsOriginalCornersApartRules() {
        composeRule.setContent {
            CornersApartTheme {
                GameScreenContent(
                    state = testUiState(),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Help").performScrollTo().performClick()

        composeRule.onNodeWithText("Goal").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Start in your corner").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Corner contact, no edge contact").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun nearbyPanelShowsEndpointAndPendingConnectionActions() {
        var connectedEndpoint: String? = null
        var acceptedEndpoint: String? = null
        var rejectedEndpoint: String? = null

        composeRule.setContent {
            CornersApartTheme {
                GameScreenContent(
                    state =
                        testUiState(
                            sessionType = SessionType.NEARBY,
                            nearbyState =
                                NearbyUiState(
                                    connectionState = ConnectionState.FAILED,
                                    discoveredEndpoints = listOf(NearbyEndpointUiState("endpoint-1", "Tablet")),
                                    pendingConnection = NearbyPendingConnection("endpoint-2", "Phone", "1234"),
                                    errorMessage = "Already discovering",
                                ),
                        ),
                    screenActions =
                        GameScreenActions(
                            onConnectToNearbyEndpoint = { endpointId -> connectedEndpoint = endpointId },
                            onAcceptPendingNearbyConnection = { endpointId -> acceptedEndpoint = endpointId },
                            onRejectPendingNearbyConnection = { endpointId -> rejectedEndpoint = endpointId },
                        ),
                )
            }
        }

        composeRule.onNodeWithText("Connect to Tablet").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Authentication code: 1234").assertIsDisplayed()
        composeRule.onNodeWithText("Already discovering").assertIsDisplayed()
        composeRule.onNodeWithText("Status: Failed").assertIsDisplayed()
        composeRule.onNodeWithText("Accept").performClick()
        composeRule.onNodeWithText("Reject").performClick()

        assertEquals("endpoint-1", connectedEndpoint)
        assertEquals("endpoint-2", acceptedEndpoint)
        assertEquals("endpoint-2", rejectedEndpoint)
    }

    @Test
    fun gameOverDialogShowsScoreBreakdown() {
        composeRule.setContent {
            CornersApartTheme {
                GameScreenContent(
                    state = testUiState(isGameOver = true),
                )
            }
        }

        composeRule.onNodeWithText("Lime wins").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Placed cells").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Bonus tiles").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Completion bonus").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Play again").assertIsDisplayed()
    }

    @Test
    fun reviewButtonStartsReviewAndHidesGameOverDialog() {
        var reviewStarts = 0
        composeRule.setContent {
            CornersApartTheme {
                GameScreenContent(
                    state = testUiState(isGameOver = true, canReviewFinishedGame = true),
                    screenActions =
                        GameScreenActions(
                            onStartMatchReview = { reviewStarts += 1 },
                        ),
                )
            }
        }

        composeRule.onNodeWithText("Review game").performClick()

        assertEquals(1, reviewStarts)
        composeRule.onAllNodesWithText("Play again").assertCountEquals(0)
    }

    @Test
    fun nearbyFinishedGameDoesNotShowReviewButton() {
        composeRule.setContent {
            CornersApartTheme {
                GameScreenContent(
                    state =
                        testUiState(
                            isGameOver = true,
                            sessionType = SessionType.NEARBY,
                            canReviewFinishedGame = false,
                        ),
                )
            }
        }

        composeRule.onAllNodesWithText("Review game").assertCountEquals(0)
    }

    private fun testUiState(
        isGameOver: Boolean = false,
        sessionType: SessionType = SessionType.LOCAL,
        nearbyState: NearbyUiState = NearbyUiState(),
        canReviewFinishedGame: Boolean = false,
    ): GameUiState {
        val selectedPiece = PieceCatalog.require(PieceCatalog.THREE_BEND_ID)
        val players =
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
                    piecesRemaining = PieceCatalog.all.size - index,
                    hasPassed = false,
                    isCurrentTurn = index == 0,
                    isComputerControlled = false,
                )
            }
        return GameUiState(
            gameMode = GameMode.FOUR_PLAYER,
            board = BoardSnapshot.empty(GameConstants.STANDARD_BOARD_SIZE),
            bonusTiles = emptyList(),
            players = players,
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
            sessionType = sessionType,
            rankedScores =
                players
                    .map { player ->
                        PlayerScore(
                            name = player.name,
                            totalScore = player.totalScore,
                            scoreBreakdown =
                                ScoreBreakdown(
                                    placedCellPoints = player.placedCellPoints,
                                    bonusTilePoints = player.bonusTilePoints,
                                    completionBonus = player.completionBonus,
                                ),
                            claimedBonusTiles = player.claimedBonusTiles,
                            colorIndex = player.colorIndex,
                            ownerIndex = player.ownerIndex,
                        )
                    }.sortedByDescending { score -> score.totalScore },
            nearbyState = nearbyState,
            canReviewFinishedGame = canReviewFinishedGame,
        )
    }
}
