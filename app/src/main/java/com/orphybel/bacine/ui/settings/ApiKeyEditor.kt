package com.orphybel.bacine.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.orphybel.bacine.R
import com.orphybel.bacine.ui.components.TvButton
import com.orphybel.bacine.ui.theme.Danger
import com.orphybel.bacine.ui.theme.Gold
import com.orphybel.bacine.ui.theme.Surface2
import com.orphybel.bacine.ui.theme.TextPrimary
import com.orphybel.bacine.ui.theme.TextSecondary

/**
 * The key entry field, shared by first-run onboarding and settings.
 *
 * The key is not masked while typing: entering a 32-character hex string with a
 * remote is painful enough without hiding what you have got so far.
 */
@Composable
fun ApiKeyEditor(
    state: SettingsState,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val fieldFocus = remember { FocusRequester() }

    LaunchedEffect(autoFocus) {
        if (autoFocus) runCatching { fieldFocus.requestFocus() }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text(stringResource(R.string.onboarding_field)) },
            singleLine = true,
            enabled = !state.checking,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold,
                unfocusedBorderColor = Surface2,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedLabelColor = Gold,
                unfocusedLabelColor = TextSecondary,
                cursorColor = Gold,
            ),
            modifier = Modifier
                .width(700.dp)
                .then(if (autoFocus) Modifier.focusRequester(fieldFocus) else Modifier),
        )

        when {
            state.checking -> Message(stringResource(R.string.onboarding_checking), TextSecondary)
            state.invalid -> Message(stringResource(R.string.onboarding_invalid), Danger)
            state.networkError != null -> Message(state.networkError, Danger)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TvButton(
                text = stringResource(R.string.onboarding_save),
                onClick = { onSave(draft) },
                enabled = !state.checking && draft.isNotBlank(),
            )
        }
    }
}

@Composable
private fun Message(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        modifier = Modifier.width(700.dp),
    )
}
