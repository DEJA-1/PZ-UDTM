package com.km.pz_app.presentation.nav.navigator

import androidx.navigation.NavOptionsBuilder
import com.km.pz_app.presentation.nav.Destination

sealed interface NavigationAction {
    data class Navigate(
        val destination: Destination,
        val navOptions: NavOptionsBuilder.() -> Unit = {}
    ) : NavigationAction

    data object NavigateUp: NavigationAction
}