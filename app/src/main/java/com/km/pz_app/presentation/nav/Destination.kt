package com.km.pz_app.presentation.nav

import kotlinx.serialization.Serializable

sealed interface Destination {
    @Serializable
    data object Home : Destination

    @Serializable
    data object RemoteTerminal : Destination
}