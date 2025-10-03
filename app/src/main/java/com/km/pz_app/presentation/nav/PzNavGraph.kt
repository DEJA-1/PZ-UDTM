package com.km.pz_app.presentation.nav

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.km.pz_app.presentation.home.homeScreen
import com.km.pz_app.presentation.remoteTerminal.remoteTerminalScreen

@Composable
fun PzNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Destination.Home,
        enterTransition = { fadeInTransition() },
        exitTransition = { fadeOutTransition() },
        popEnterTransition = {
            fadeInTransition() + scaleIn(
                initialScale = 1.024f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
        },
        popExitTransition = {
            fadeOutTransition() + scaleOut(
                targetScale = 1.024f,
                animationSpec = tween(
                    durationMillis = 100,
                    easing = FastOutLinearInEasing
                )
            )
        }
    ) {
        homeScreen()
        remoteTerminalScreen()
    }
}

private fun fadeInTransition() = fadeIn(
    animationSpec = tween(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    )
)

private fun fadeOutTransition() = fadeOut(
    animationSpec = tween(
        durationMillis = 150,
        easing = FastOutLinearInEasing
    )
)