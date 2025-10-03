package com.km.pz_app.presentation.nav.navigator

import androidx.navigation.NavOptionsBuilder
import com.km.pz_app.presentation.nav.Destination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class DefaultNavigator(
    override val startDestination: Destination,
) : INavigator {
    private val navigationChannel: Channel<NavigationAction> = Channel(Channel.CONFLATED)
    override val navigationFlow: Flow<NavigationAction> = navigationChannel.receiveAsFlow()

    override fun pushNavigationEvent(
        destination: Destination,
        navOptions: NavOptionsBuilder.() -> Unit
    ) {
        navigationChannel.trySend(
            NavigationAction.Navigate(destination = destination, navOptions = navOptions)
        )
    }

    override fun navigateUp() {
        navigationChannel.trySend(NavigationAction.NavigateUp)
    }
}