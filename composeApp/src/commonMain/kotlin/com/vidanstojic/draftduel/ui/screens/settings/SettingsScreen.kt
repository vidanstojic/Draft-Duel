package com.vidanstojic.draftduel.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidanstojic.draftduel.di.LocalAppContainer
import com.vidanstojic.draftduel.feedback.GameSettings
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.app_name
import com.vidanstojic.draftduel.resources.back
import com.vidanstojic.draftduel.resources.how_to_play
import com.vidanstojic.draftduel.resources.settings
import com.vidanstojic.draftduel.resources.settings_music
import com.vidanstojic.draftduel.resources.settings_music_hint
import com.vidanstojic.draftduel.resources.settings_sound_label
import com.vidanstojic.draftduel.resources.settings_sounds
import com.vidanstojic.draftduel.resources.settings_sounds_hint
import com.vidanstojic.draftduel.resources.settings_vibration
import com.vidanstojic.draftduel.resources.settings_vibration_hint
import com.vidanstojic.draftduel.ui.components.DdIconButton
import com.vidanstojic.draftduel.ui.components.DdScreen
import com.vidanstojic.draftduel.ui.components.DdTopBar
import com.vidanstojic.draftduel.ui.components.SectionLabel
import com.vidanstojic.draftduel.ui.components.TopBarTitle
import com.vidanstojic.draftduel.ui.surface.SurfaceStyle
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SettingsRoute(onBack: () -> Unit, onHowToPlay: () -> Unit) {
    val container = LocalAppContainer.current
    val repository = container.settingsRepository
    val settings by repository.settings.collectAsStateWithLifecycle()
    SettingsScreen(
        settings = settings,
        versionLabel = container.appInfo.label,
        onBack = onBack,
        onMusic = { on ->
            repository.setMusic(on)
            container.feedback.applyMusic()
        },
        onSounds = repository::setSounds,
        onVibration = repository::setVibration,
        onHowToPlay = onHowToPlay,
    )
}

@Composable
fun SettingsScreen(
    settings: GameSettings,
    versionLabel: String,
    onBack: () -> Unit,
    onMusic: (Boolean) -> Unit,
    onSounds: (Boolean) -> Unit,
    onVibration: (Boolean) -> Unit,
    onHowToPlay: () -> Unit,
) {
    val colors = DraftDuelTheme.colors
    DdScreen(surface = SurfaceStyle.TABLE) {
        DdTopBar(
            leading = { DdIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back), onBack) },
            title = { TopBarTitle(stringResource(Res.string.app_name), stringResource(Res.string.settings)) },
        )
        SectionLabel(stringResource(Res.string.settings_sound_label), Modifier.padding(start = 4.dp, top = 6.dp))
        Panel {
            SettingSwitch(stringResource(Res.string.settings_music), stringResource(Res.string.settings_music_hint), settings.music, onMusic)
            SettingSwitch(stringResource(Res.string.settings_sounds), stringResource(Res.string.settings_sounds_hint), settings.sounds, onSounds)
            SettingSwitch(stringResource(Res.string.settings_vibration), stringResource(Res.string.settings_vibration_hint), settings.vibration, onVibration)
        }
        Panel {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onHowToPlay)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(Res.string.how_to_play), style = DraftDuelTheme.type.bodyStrong, color = colors.onScreen, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.accent)
            }
        }
        Spacer(Modifier.weight(1f))
        Text(versionLabel, style = DraftDuelTheme.type.mono, color = colors.muted.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    val colors = DraftDuelTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.panel)
            .border(1.5.dp, colors.outline, shape)
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) { content() }
}

@Composable
private fun SettingSwitch(title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = DraftDuelTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = DraftDuelTheme.type.bodyStrong, color = colors.onScreen)
            Text(hint, style = DraftDuelTheme.type.caption, color = colors.muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = colors.onAccent),
        )
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    DraftDuelTheme {
        SettingsScreen(GameSettings(), "v0.1.0 (1)", {}, {}, {}, {}, {})
    }
}
