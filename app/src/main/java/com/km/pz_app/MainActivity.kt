package com.km.pz_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.km.pz_app.presentation.home.homeScreen
import com.km.pz_app.presentation.nav.Destination
import com.km.pz_app.presentation.nav.PzNavGraph
import com.km.pz_app.presentation.nav.navigator.INavigator
import com.km.pz_app.presentation.nav.navigator.NavigationAction
import com.km.pz_app.presentation.utils.ObserveFlowWithLifecycle
import com.km.pz_app.ui.theme.PZAPPTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navigator: INavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            enableEdgeToEdge()
            PZAPPTheme {
                val navController = rememberNavController()
                ObserveFlowWithLifecycle(flow = navigator.navigationFlow) {
                    handleNavigationEvent(navController = navController, navAction = it)
                }

                PzNavGraph(navController = navController)
            }
        }
    }
}

private fun handleNavigationEvent(
    navController: NavController,
    navAction: NavigationAction,
) {
    when (navAction) {
        is NavigationAction.Navigate -> navController.navigate(
            navAction.destination
        ) { navAction.navOptions }

        NavigationAction.NavigateUp -> navController.navigateUp()
    }
}