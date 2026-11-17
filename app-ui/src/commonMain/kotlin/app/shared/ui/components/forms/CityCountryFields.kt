package app.shared.ui.components.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.shared.ui.resources.Res
import app.shared.ui.resources.simt_ui_city_name
import app.shared.ui.resources.simt_ui_country_code_label
import app.shared.ui.resources.simt_ui_country_code_placeholder
import app.shared.ui.resources.simt_ui_enter_city_name
import org.jetbrains.compose.resources.stringResource

@Composable
fun CityCountryFields(
    city: String,
    onCityChange: (String) -> Unit,
    countryCode: String,
    onCountryCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ClearableTextField(
            value = city,
            onValueChange = onCityChange,
            label = stringResource(Res.string.simt_ui_city_name),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = stringResource(Res.string.simt_ui_enter_city_name),
            maxLength = CITY_NAME_MAX_LENGTH,
        )

        Spacer(modifier = Modifier.height(12.dp))

        ClearableTextField(
            value = countryCode,
            onValueChange = { input -> onCountryCodeChange(input.uppercase()) },
            label = stringResource(Res.string.simt_ui_country_code_label),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = stringResource(Res.string.simt_ui_country_code_placeholder),
            maxLength = COUNTRY_CODE_LENGTH,
        )
    }
}

const val CITY_NAME_MAX_LENGTH = 30

const val COUNTRY_CODE_LENGTH = 2
