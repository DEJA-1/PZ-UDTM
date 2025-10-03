package com.km.pz_app.presentation.nav.navigator

import androidx.navigation.NavOptionsBuilder
import com.km.pz_app.presentation.nav.Destination
import kotlinx.coroutines.flow.Flow

interface INavigator {
    val startDestination: Destination
    val navigationFlow: Flow<NavigationAction>

    fun pushNavigationEvent(
        destination: Destination,
        navOptions: NavOptionsBuilder.() -> Unit = {},
    )

    fun navigateUp()
}