package com.vidanstojic.draftduel.ui.screens.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vidanstojic.draftduel.domain.match.HandOverRule
import com.vidanstojic.draftduel.domain.match.MatchRules
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.custom_budget
import com.vidanstojic.draftduel.resources.custom_cards
import com.vidanstojic.draftduel.resources.custom_first_cost
import com.vidanstojic.draftduel.resources.custom_hand_over
import com.vidanstojic.draftduel.resources.custom_increment
import com.vidanstojic.draftduel.resources.custom_less
import com.vidanstojic.draftduel.resources.custom_more
import com.vidanstojic.draftduel.resources.custom_off
import com.vidanstojic.draftduel.resources.custom_ratings_visible
import com.vidanstojic.draftduel.resources.custom_recipe_note
import com.vidanstojic.draftduel.resources.custom_reset
import com.vidanstojic.draftduel.resources.custom_seconds
import com.vidanstojic.draftduel.resources.custom_slots
import com.vidanstojic.draftduel.resources.custom_turn
import com.vidanstojic.draftduel.resources.rules_classic
import com.vidanstojic.draftduel.resources.rules_classic_hint
import com.vidanstojic.draftduel.resources.rules_custom
import com.vidanstojic.draftduel.resources.rules_custom_hint
import com.vidanstojic.draftduel.resources.rules_custom_quick_match
import com.vidanstojic.draftduel.resources.rules_default
import com.vidanstojic.draftduel.resources.rules_default_hint
import com.vidanstojic.draftduel.resources.rules_label
import com.vidanstojic.draftduel.ui.components.SectionLabel
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Rules of the match: Default (what the game is tested with), Classic, or Custom, where every
 * parameter of [MatchRules] can be set for play-testing.
 */
@Composable
internal fun RulesSection(
    state: CategoryUiState,
    onPresetSelected: (RulesPreset) -> Unit,
    onCustomChange: ((MatchRules) -> MatchRules) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(Res.string.rules_label), Modifier.padding(start = 4.dp, top = 6.dp))
        PresetSelector(selected = state.rulesPreset, customAllowed = state.customRulesAllowed, onSelected = onPresetSelected)
        Caption(stringResource(state.rulesPreset.description()))
        if (!state.customRulesAllowed) Caption(stringResource(Res.string.rules_custom_quick_match))
        if (state.rulesPreset == RulesPreset.CUSTOM) {
            CustomRulesEditor(rules = state.customRules, isRecipe = state.isRecipe, onChange = onCustomChange)
        } else if (state.isRecipe && state.rulesPreset == RulesPreset.DEFAULT) {
            Caption(stringResource(Res.string.custom_recipe_note))
        }
    }
}

@Composable
private fun PresetSelector(
    selected: RulesPreset,
    customAllowed: Boolean,
    onSelected: (RulesPreset) -> Unit,
) {
    val colors = DraftDuelTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.panel)
            .border(1.5.dp, colors.outline, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RulesPreset.entries.forEach { preset ->
            val isSelected = preset == selected
            val enabled = preset != RulesPreset.CUSTOM || customAllowed
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) colors.accent else colors.panel.copy(alpha = 0f))
                    .selectable(selected = isSelected, enabled = enabled, role = Role.Tab, onClick = { onSelected(preset) })
                    .alpha(if (enabled) 1f else 0.4f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(preset.label()),
                    style = DraftDuelTheme.type.bodyStrong,
                    color = if (isSelected) colors.onAccent else colors.onScreen,
                )
            }
        }
    }
}

@Composable
private fun CustomRulesEditor(
    rules: MatchRules,
    isRecipe: Boolean,
    onChange: ((MatchRules) -> MatchRules) -> Unit,
) {
    val colors = DraftDuelTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.panel)
            .border(1.5.dp, colors.outline, shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        StepperRow(
            label = stringResource(Res.string.custom_budget),
            value = rules.budget.toString(),
            canDecrease = rules.budget - BUDGET_STEP in MatchRules.BUDGET_RANGE,
            canIncrease = rules.budget + BUDGET_STEP in MatchRules.BUDGET_RANGE,
            onDecrease = { onChange { it.copy(budget = it.budget - BUDGET_STEP) } },
            onIncrease = { onChange { it.copy(budget = it.budget + BUDGET_STEP) } },
        )
        StepperRow(
            label = stringResource(Res.string.custom_cards),
            value = rules.cards.toString(),
            canDecrease = rules.cards - CARDS_STEP in MatchRules.CARDS_RANGE,
            canIncrease = rules.cards + CARDS_STEP in MatchRules.CARDS_RANGE,
            onDecrease = { onChange { it.copy(cards = it.cards - CARDS_STEP) } },
            onIncrease = { onChange { it.copy(cards = it.cards + CARDS_STEP) } },
        )
        if (!isRecipe) {
            val slots = rules.slots
            StepperRow(
                label = stringResource(Res.string.custom_slots),
                value = slots?.toString() ?: stringResource(Res.string.custom_off),
                canDecrease = slots != null,
                canIncrease = slots == null || slots < MatchRules.SLOTS_RANGE.last,
                // Below the smallest limit comes "off"; above "off" comes the smallest limit.
                onDecrease = { onChange { it.copy(slots = it.slots?.let { s -> if (s == MatchRules.SLOTS_RANGE.first) null else s - 1 }) } },
                onIncrease = { onChange { it.copy(slots = it.slots?.plus(1) ?: MatchRules.SLOTS_RANGE.first) } },
            )
            val handOver = rules.handOver
            SwitchRow(
                label = stringResource(Res.string.custom_hand_over),
                checked = handOver != null,
                onCheckedChange = { on -> onChange { it.copy(handOver = if (on) HandOverRule() else null) } },
            )
            if (handOver != null) {
                StepperRow(
                    label = stringResource(Res.string.custom_first_cost),
                    value = handOver.firstCost.toString(),
                    canDecrease = handOver.firstCost - 1 in HandOverRule.FIRST_COST_RANGE,
                    canIncrease = handOver.firstCost + 1 in HandOverRule.FIRST_COST_RANGE,
                    onDecrease = { onChange { it.copy(handOver = handOver.copy(firstCost = handOver.firstCost - 1)) } },
                    onIncrease = { onChange { it.copy(handOver = handOver.copy(firstCost = handOver.firstCost + 1)) } },
                )
                StepperRow(
                    label = stringResource(Res.string.custom_increment),
                    value = "+${handOver.increment}",
                    canDecrease = handOver.increment - 1 in HandOverRule.INCREMENT_RANGE,
                    canIncrease = handOver.increment + 1 in HandOverRule.INCREMENT_RANGE,
                    onDecrease = { onChange { it.copy(handOver = handOver.copy(increment = handOver.increment - 1)) } },
                    onIncrease = { onChange { it.copy(handOver = handOver.copy(increment = handOver.increment + 1)) } },
                )
            }
        }
        StepperRow(
            label = stringResource(Res.string.custom_turn),
            value = stringResource(Res.string.custom_seconds, rules.turnSeconds),
            canDecrease = rules.turnSeconds - TURN_STEP in MatchRules.TURN_SECONDS_RANGE,
            canIncrease = rules.turnSeconds + TURN_STEP in MatchRules.TURN_SECONDS_RANGE,
            onDecrease = { onChange { it.copy(turnSeconds = it.turnSeconds - TURN_STEP) } },
            onIncrease = { onChange { it.copy(turnSeconds = it.turnSeconds + TURN_STEP) } },
        )
        SwitchRow(
            label = stringResource(Res.string.custom_ratings_visible),
            checked = rules.ratingsVisible,
            onCheckedChange = { on -> onChange { it.copy(ratingsVisible = on) } },
        )
        if (isRecipe) Caption(stringResource(Res.string.custom_recipe_note))
        TextButton(onClick = { onChange { MatchRules.DEFAULT } }, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(Res.string.custom_reset), style = DraftDuelTheme.type.mono, color = colors.accent)
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    val colors = DraftDuelTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = DraftDuelTheme.type.body, color = colors.onScreen, modifier = Modifier.weight(1f))
        IconButton(onClick = onDecrease, enabled = canDecrease) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(Res.string.custom_less, label),
                tint = if (canDecrease) colors.accent else colors.muted.copy(alpha = 0.4f),
            )
        }
        Text(
            text = value,
            style = DraftDuelTheme.type.monoStrong,
            color = colors.onScreen,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 40.dp),
        )
        IconButton(onClick = onIncrease, enabled = canIncrease) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(Res.string.custom_more, label),
                tint = if (canIncrease) colors.accent else colors.muted.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = DraftDuelTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = DraftDuelTheme.type.body, color = colors.onScreen, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = colors.onAccent),
        )
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text = text,
        style = DraftDuelTheme.type.caption,
        color = DraftDuelTheme.colors.muted,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

private fun RulesPreset.label(): StringResource = when (this) {
    RulesPreset.DEFAULT -> Res.string.rules_default
    RulesPreset.CLASSIC -> Res.string.rules_classic
    RulesPreset.CUSTOM -> Res.string.rules_custom
}

private fun RulesPreset.description(): StringResource = when (this) {
    RulesPreset.DEFAULT -> Res.string.rules_default_hint
    RulesPreset.CLASSIC -> Res.string.rules_classic_hint
    RulesPreset.CUSTOM -> Res.string.rules_custom_hint
}

private const val BUDGET_STEP = 5
private const val CARDS_STEP = 2
private const val TURN_STEP = 5
