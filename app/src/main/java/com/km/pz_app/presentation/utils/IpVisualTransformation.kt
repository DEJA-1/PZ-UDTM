package com.km.pz_app.presentation.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

object IpVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val filtered = text.text.filter { it.isDigit() || it == '.' }
        val limited = filtered.take(15)

        val out = AnnotatedString(text = limited)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                offset.coerceAtMost(limited.length)

            override fun transformedToOriginal(offset: Int): Int =
                offset.coerceAtMost(limited.length)
        }

        return TransformedText(
            text = out,
            offsetMapping = offsetMapping
        )
    }
}