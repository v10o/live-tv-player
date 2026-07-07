package com.iptvplayer.tv.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.iptvplayer.tv.ui.screens.channels.ChannelsScreen
import com.iptvplayer.tv.ui.screens.epg.EpgScreen
import com.iptvplayer.tv.ui.screens.home.HomeScreen
import com.iptvplayer.tv.ui.screens.vod.VodViewModel
import com.iptvplayer.tv.ui.screens.player.DirectPlayerScreen
import com.iptvplayer.tv.ui.screens.player.PlayerScreen
import com.iptvplayer.tv.ui.screens.player.VodPlayerScreen
import com.iptvplayer.tv.ui.screens.series.SeriesDetailScreen
import com.iptvplayer.tv.ui.screens.series.SeriesScreen
import com.iptvplayer.tv.ui.screens.settings.SettingsScreen
import com.iptvplayer.tv.ui.screens.vod.VodScreen
import com.iptvplayer.tv.ui.screens.vod.VodDetailScreen
import com.iptvplayer.tv.ui.screens.search.SearchScreen
import com.iptvplayer.tv.ui.screens.watchlist.WatchlistScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val CHANNELS = "channels/{playlistId}"
    const val EPG = "epg/{playlistId}"
    const val VOD = "vod/{playlistId}"
    const val SERIES = "series/{playlistId}"
    const val SERIES_DETAIL = "series_detail/{playlistId}/{seriesId}"
    const val PLAYER = "player/{channelId}"
    const val VOD_PLAYER = "vod_player/{title}?url={url}"
    const val DIRECT_PLAYER = "direct_player"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val WATCHLIST = "watchlist"
    const val VOD_DETAIL = "vod_detail/{vodId}"
    const val VOD_DETAIL_BY_TITLE = "vod_detail_title/{title}"

    fun channels(playlistId: String) = "channels/$playlistId"
    fun epg(playlistId: String) = "epg/$playlistId"
    fun vod(playlistId: String) = "vod/$playlistId"
    fun series(playlistId: String) = "series/$playlistId"
    fun seriesDetail(playlistId: String, seriesId: Int) = "series_detail/$playlistId/$seriesId"
    fun vodDetail(vodId: String) = "vod_detail/$vodId"
    fun vodDetailByTitle(title: String) = "vod_detail_title/${URLEncoder.encode(title, "UTF-8")}"
    fun player(channelId: String) = "player/$channelId"
    fun vodPlayer(title: String, url: String) = "vod_player/${URLEncoder.encode(title, "UTF-8")}?url=${URLEncoder.encode(url, "UTF-8")}"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            val vodViewModel: VodViewModel = hiltViewModel()
            HomeScreen(
                onPlaylistClick = { playlistId ->
                    navController.navigate(Routes.channels(playlistId))
                },
                onVodClick = { playlistId ->
                    navController.navigate(Routes.vod(playlistId))
                },
                onSeriesClick = { playlistId ->
                    navController.navigate(Routes.series(playlistId))
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                },
                onSearchClick = {
                    navController.navigate(Routes.SEARCH)
                },
                onWatchlistClick = {
                    navController.navigate(Routes.WATCHLIST)
                },
                onGuideClick = { playlistId ->
                    navController.navigate(Routes.epg(playlistId))
                },
                onVodItemClick = { vodItem ->
                    // Navigate to movie detail page
                    navController.navigate(Routes.vodDetail(vodItem.id))
                },
                onSeriesItemClick = { seriesItem ->
                    // Navigate to series detail page
                    navController.navigate(Routes.seriesDetail(seriesItem.playlistId, seriesItem.seriesId))
                },
                onWatchlistItemClick = { watchlistItem ->
                    // Navigate based on watchlist item type
                    if (watchlistItem.type == "vod") {
                        navController.navigate(Routes.vodDetail(watchlistItem.itemId))
                    } else {
                        // For series, itemId is the seriesId stored as String
                        val seriesId = watchlistItem.itemId.toIntOrNull() ?: 0
                        navController.navigate(Routes.seriesDetail(watchlistItem.playlistId, seriesId))
                    }
                },
                onTmdbMovieClick = { title ->
                    navController.navigate(Routes.vodDetailByTitle(title))
                }
            )
        }

        composable(
            route = Routes.CHANNELS,
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
            ChannelsScreen(
                playlistId = playlistId,
                onChannelClick = { channelId ->
                    navController.navigate(Routes.player(channelId))
                },
                onBackPress = { navController.popBackStack() }
            )
        }

        // EPG / TV Guide
        composable(
            route = Routes.EPG,
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
            EpgScreen(
                playlistId = playlistId,
                onChannelClick = { channelId ->
                    navController.navigate(Routes.player(channelId))
                },
                onBackPress = { navController.popBackStack() },
                onHomeClick = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
                onMoviesClick = { navController.navigate(Routes.vod(playlistId)) { popUpTo(Routes.EPG) { inclusive = true } } },
                onShowsClick = { navController.navigate(Routes.series(playlistId)) { popUpTo(Routes.EPG) { inclusive = true } } },
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }

        // VOD Movies
        composable(
            route = Routes.VOD,
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
            VodScreen(
                playlistId = playlistId,
                onMovieClick = { vodItem, _ ->
                    navController.navigate(Routes.vodDetail(vodItem.id))
                },
                onBackPress = { navController.popBackStack() },
                onHomeClick = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
                onLiveTVClick = { navController.navigate(Routes.epg(playlistId)) { popUpTo(Routes.VOD) { inclusive = true } } },
                onShowsClick = { navController.navigate(Routes.series(playlistId)) { popUpTo(Routes.VOD) { inclusive = true } } },
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }

        // VOD Detail
        composable(
            route = Routes.VOD_DETAIL,
            arguments = listOf(navArgument("vodId") { type = NavType.StringType })
        ) { backStackEntry ->
            val vodId = backStackEntry.arguments?.getString("vodId") ?: return@composable
            VodDetailScreen(
                vodId = vodId,
                onPlayClick = { title, url ->
                    navController.navigate(Routes.vodPlayer(title, url))
                },
                onBackPress = { navController.popBackStack() }
            )
        }

        // VOD Detail by Title (for TMDB matches)
        composable(
            route = Routes.VOD_DETAIL_BY_TITLE,
            arguments = listOf(navArgument("title") { type = NavType.StringType })
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title")?.let {
                URLDecoder.decode(it, "UTF-8")
            } ?: return@composable
            VodDetailScreen(
                vodId = null,
                searchTitle = title,
                onPlayClick = { movieTitle, url ->
                    navController.navigate(Routes.vodPlayer(movieTitle, url))
                },
                onBackPress = { navController.popBackStack() }
            )
        }

        // Series
        composable(
            route = Routes.SERIES,
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
            SeriesScreen(
                playlistId = playlistId,
                onSeriesClick = { seriesItem ->
                    navController.navigate(Routes.seriesDetail(playlistId, seriesItem.seriesId))
                },
                onBackPress = { navController.popBackStack() },
                onHomeClick = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
                onLiveTVClick = { navController.navigate(Routes.epg(playlistId)) { popUpTo(Routes.SERIES) { inclusive = true } } },
                onMoviesClick = { navController.navigate(Routes.vod(playlistId)) { popUpTo(Routes.SERIES) { inclusive = true } } },
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }

        // Series Detail
        composable(
            route = Routes.SERIES_DETAIL,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.StringType },
                navArgument("seriesId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
            val seriesId = backStackEntry.arguments?.getInt("seriesId") ?: return@composable
            SeriesDetailScreen(
                playlistId = playlistId,
                seriesId = seriesId,
                onEpisodeClick = { title, url ->
                    navController.navigate(Routes.vodPlayer(title, url))
                },
                onBackPress = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(navArgument("channelId") { type = NavType.StringType })
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId") ?: return@composable
            PlayerScreen(
                channelId = channelId,
                onBackPress = { navController.popBackStack() }
            )
        }

        // VOD Player (direct URL playback)
        composable(
            route = Routes.VOD_PLAYER,
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title")?.let {
                URLDecoder.decode(it, "UTF-8")
            } ?: "Movie"
            val url = backStackEntry.arguments?.getString("url")?.let {
                URLDecoder.decode(it, "UTF-8")
            } ?: return@composable

            VodPlayerScreen(
                title = title,
                streamUrl = url,
                onBackPress = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBackPress = { navController.popBackStack() }
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onChannelClick = { channel ->
                    navController.navigate(Routes.player(channel.id))
                },
                onVodClick = { vodItem, _ ->
                    navController.navigate(Routes.vodDetail(vodItem.id))
                },
                onSeriesClick = { seriesItem ->
                    navController.navigate(Routes.seriesDetail(seriesItem.playlistId, seriesItem.seriesId))
                },
                onBackPress = { navController.popBackStack() }
            )
        }

        composable(Routes.WATCHLIST) {
            WatchlistScreen(
                onItemClick = { watchlistItem ->
                    if (watchlistItem.type == "vod") {
                        navController.navigate(Routes.vodDetail(watchlistItem.itemId))
                    } else {
                        val seriesId = watchlistItem.itemId.toIntOrNull() ?: 0
                        navController.navigate(Routes.seriesDetail(watchlistItem.playlistId, seriesId))
                    }
                },
                onBackPress = { navController.popBackStack() },
                onHomeClick = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
                onLiveTVClick = {
                    // Need a playlist for Live TV - go home if none
                    navController.navigate(Routes.HOME)
                },
                onMoviesClick = {
                    navController.navigate(Routes.HOME)
                },
                onShowsClick = {
                    navController.navigate(Routes.HOME)
                },
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.DIRECT_PLAYER) {
            DirectPlayerScreen(
                onBackPress = { navController.popBackStack() }
            )
        }
    }
}
