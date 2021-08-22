package com.m3sv.plainupnp.compose.widgets

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.m3sv.plainupnp.common.util.pass
import com.m3sv.plainupnp.compose.util.AppTheme
import com.m3sv.plainupnp.interfaces.LifecycleState
import kotlin.system.exitProcess

@Composable
fun Activity.LifecycleIndicator(lifecycleState: LifecycleState) {
    if (!(lifecycleState == LifecycleState.FINISH || lifecycleState == LifecycleState.CLOSE))
        return

    FadedBackground {
        when (lifecycleState) {
            LifecycleState.FINISH -> FinishIndicator()
            LifecycleState.CLOSE ->
                CloseIndicator {
                    finishAffinity()
                    exitProcess(0)
                }
            else -> pass
        }
    }
}

@Composable
fun FadedBackground(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colors.onBackground.copy(alpha = 0.5f))
            .fillMaxSize()
    ) {
        Card(
            modifier = Modifier
                .width(200.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(AppTheme.cornerRadius)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                content()
            }
        }
    }
}

@Composable
private fun FinishIndicator() {
    Text(
        text = "The application is finishing",
        textAlign = TextAlign.Center,
        style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
        modifier = Modifier.padding(16.dp)
    )

    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
}

@Composable
private fun CloseIndicator(onCloseClick: () -> Unit) {
    Text(
        text = "The application has been finished",
        textAlign = TextAlign.Center,
        style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
        modifier = Modifier.padding(16.dp)
    )

    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.10f))
    TextButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        onClick = onCloseClick
    ) {
        Text("Close", fontSize = 16.sp)
    }
}
