package app.simt.ui.components.forms

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_stepper_decrement
import app.simt.ui.resources.simt_ui_stepper_increment
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

private const val DEBOUNCE_MS = 500L

/** Breathing room between the centered value and the min/max edge hints. */
private val StepperHintInnerSpacing = 8.dp

/**
 * An integer value with long-press-repeating +/- stepper buttons and, when [range] is
 * given, tappable min/max edge hints.
 *
 * @param range Inclusive bounds the value is coerced into. When `null` (the default) the
 *   value is unbounded: no edge hints are shown, typed values are not coerced, and the
 *   stepper buttons are always enabled.
 * @param debounceMs Delay before [onValueChange] fires after rapid stepping.
 *   The displayed value updates instantly; the callback is debounced so callers
 *   (e.g. repository writes) are not invoked on every intermediate step.
 *   Set to 0 to disable debouncing.
 * @param tapToEdit When `true`, the value can also be set by tapping it: the field takes
 *   focus, opens a numbers-only keyboard with a Done action, and typed digits are parsed,
 *   coerced into [range] when one is given, and propagated through the same [debounceMs]
 *   debounce as the stepper buttons. An unfinished edit (e.g. an emptied field) reverts to
 *   the last valid value when the field loses focus. When `false` (the default) the field
 *   is read-only and behaves exactly as a plain stepper.
 */
@Composable
public fun NumberStepperField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange? = null,
    modifier: Modifier = Modifier,
    suffix: String = "",
    step: Int = 1,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    debounceMs: Long = DEBOUNCE_MS,
    tapToEdit: Boolean = false,
) {
    val min = range?.first
    val max = range?.last
    val focusManager = LocalFocusManager.current

    // Local display value for instant UI feedback; syncs from the parent value
    // whenever the parent-supplied value changes externally.
    var displayValue by remember { mutableStateOf(if (range == null) value else value.coerceIn(range)) }
    LaunchedEffect(value) { displayValue = if (range == null) value else value.coerceIn(range) }

    val canDecrement = min == null || displayValue > min
    val canIncrement = max == null || displayValue < max

    val latestOnValueChange by rememberUpdatedState(onValueChange)

    // Debounce channel: every time displayValue changes locally we restart the timer.
    // When the timer elapses, we propagate the latest value to the caller.
    var pendingValue by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(pendingValue) {
        val pending = pendingValue ?: return@LaunchedEffect
        if (debounceMs > 0) delay(debounceMs.milliseconds)
        latestOnValueChange(pending)
        pendingValue = null
    }

    fun applyStep(next: Int) {
        val coerced = range?.let { next.coerceIn(it) } ?: next
        displayValue = coerced
        pendingValue = coerced
    }

    // Tap-to-edit: the raw text while the field is focused. On every focus change the text
    // is re-seeded from [displayValue] — seeding on focus gain, reverting on focus loss — so
    // an unfinished edit ("" or a partially typed number) can never stick.
    var isFocused by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(displayValue.toString()) }

    OutlinedTextField(
        value = if (tapToEdit && isFocused) editText else displayValue.toString(),
        onValueChange = { newText ->
            if (!tapToEdit) return@OutlinedTextField
            editText = newText
            // Parse and ride the same debounce as the stepper buttons (coercion into
            // [range] happens in applyStep); an unparseable edit keeps the last valid
            // value on screen.
            newText.trim().toIntOrNull()?.let { applyStep(it) }
        },
        readOnly = !tapToEdit,
        keyboardOptions =
            if (tapToEdit) {
                KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
            } else {
                KeyboardOptions.Default
            },
        enabled = enabled,
        label = { Text(label) },
        prefix =
            if (range != null) {
                {
                    RangeHintText(
                        text = "${range.first} ≤",
                        modifier = Modifier.padding(end = StepperHintInnerSpacing),
                        onClick =
                            if (enabled && displayValue != range.first) {
                                { applyStep(range.first) }
                            } else {
                                null
                            },
                    )
                }
            } else {
                null
            },
        suffix =
            if (range != null) {
                {
                    RangeHintText(
                        text = "≤ ${range.last}$suffix",
                        modifier = Modifier.padding(start = StepperHintInnerSpacing),
                        onClick =
                            if (enabled && displayValue != range.last) {
                                { applyStep(range.last) }
                            } else {
                                null
                            },
                    )
                }
            } else {
                null
            },
        textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
        singleLine = true,
        keyboardActions =
            KeyboardActions(
                onDone = {
                    if (tapToEdit) focusManager.clearFocus()
                },
            ),
        modifier =
            modifier
                .then(
                    if (tapToEdit) {
                        Modifier
                    } else {
                        Modifier.focusProperties { canFocus = false }
                    },
                )
                .onFocusChanged { focusState ->
                    if (tapToEdit) {
                        isFocused = focusState.isFocused
                        editText = displayValue.toString()
                    }
                },
        supportingText = supportingText,
        trailingIcon = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Column {
                    StepperIconButton(
                        enabled = enabled && canIncrement,
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = stringResource(Res.string.simt_ui_stepper_increment),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onStep = { applyStep(displayValue + step) },
                        canStepNow = { max == null || displayValue < max },
                    )

                    StepperIconButton(
                        enabled = enabled && canDecrement,
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = stringResource(Res.string.simt_ui_stepper_decrement),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onStep = { applyStep(displayValue - step) },
                        canStepNow = { min == null || displayValue > min },
                    )
                }
            }
        },
    )
}

@Composable
private fun StepperIconButton(
    enabled: Boolean,
    icon: @Composable () -> Unit,
    onStep: () -> Unit,
    canStepNow: () -> Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // Prevent an extra "click" increment when releasing after a long press.
    var didRepeat by remember { mutableStateOf(false) }

    LaunchedEffect(pressed, enabled) {
        if (!pressed || !enabled) return@LaunchedEffect

        didRepeat = false

        // Initial delay before repeating.
        delay(350.milliseconds)

        while (enabled && canStepNow()) {
            didRepeat = true
            onStep()
            delay(80.milliseconds)
        }
    }

    IconButton(
        onClick = {
            if (!enabled) return@IconButton
            if (didRepeat) {
                didRepeat = false
                return@IconButton
            }
            if (!canStepNow()) return@IconButton
            onStep()
        },
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier.size(36.dp),
    ) {
        icon()
    }
}

@Composable
private fun RangeHintText(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier =
            if (onClick != null) {
                modifier.pointerInput(onClick) {
                    detectTapGestures { onClick() }
                }
            } else {
                modifier
            },
    )
}
