package com.finnvek.cornersapart.data

import android.content.Context
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.multiplayer.ConnectionsClientFacade
import com.finnvek.cornersapart.multiplayer.NearbyConnectionsCoordinator
import com.finnvek.cornersapart.multiplayer.PlayServicesConnectionsClientFacade
import com.finnvek.cornersapart.opponents.ComputerOpponentEngine
import com.finnvek.cornersapart.review.GameReplayer
import com.finnvek.cornersapart.review.MatchReviewAnalyzer
import com.finnvek.cornersapart.runtime.StringProvider
import com.finnvek.cornersapart.runtime.SystemTimeProvider
import com.finnvek.cornersapart.runtime.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GameRuntimeModule {
    @Provides
    @Singleton
    fun provideGameEngine(): GameEngine = GameEngine()

    @Provides
    @Singleton
    fun provideComputerOpponentEngine(gameEngine: GameEngine): ComputerOpponentEngine =
        ComputerOpponentEngine(gameEngine = gameEngine)

    @Provides
    @Singleton
    fun provideGameReplayer(gameEngine: GameEngine): GameReplayer = GameReplayer(gameEngine)

    @Provides
    @Singleton
    fun provideMatchReviewAnalyzer(
        gameEngine: GameEngine,
        gameReplayer: GameReplayer,
    ): MatchReviewAnalyzer =
        MatchReviewAnalyzer(
            gameEngine = gameEngine,
            gameReplayer = gameReplayer,
            dispatcher = Dispatchers.Default,
        )

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = SystemTimeProvider()

    @Provides
    @Singleton
    fun provideStringProvider(
        @ApplicationContext context: Context,
    ): StringProvider = StringProvider { resourceId -> context.getString(resourceId) }

    @Provides
    @Singleton
    fun provideConnectionsClientFacade(
        @ApplicationContext context: Context,
    ): ConnectionsClientFacade = PlayServicesConnectionsClientFacade(context)

    @Provides
    @Singleton
    fun provideNearbyConnectionsCoordinator(
        facade: ConnectionsClientFacade,
        gameEngine: GameEngine,
        @ApplicationContext context: Context,
    ): NearbyConnectionsCoordinator =
        NearbyConnectionsCoordinator(
            facade = facade,
            gameEngine = gameEngine,
            localEndpointName = context.getString(R.string.app_name),
        )
}
