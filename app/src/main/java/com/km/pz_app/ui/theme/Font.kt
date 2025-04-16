package com.km.pz_app.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.GoogleFont.Provider
import com.km.pz_app.R

val poppinsFont = GoogleFont("Poppins")

val poppinsProvider = Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

val PoppinsFontFamily = FontFamily(
    Font(googleFont = poppinsFont, fontProvider = poppinsProvider)
)