package com.holfuy.configtool.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RepositoryConfigurationScreen(
    onContinue: () -> Unit
)
{
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {

        Text(
            text = "Holfuy Upgrader",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Firmware Repository Configuration",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Holfuy Upgrader downloads firmware files from Holfuy " + 
            "and stores them in a firmware repository that you choose."
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Please select (or create) a folder to use as " +
            "the firmware repository."
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "We recommend creating a folder named " +
            "\"HolfuyFirmware\" inside your \"Download\" folder."
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onContinue
        ) {
            Text("Continue")
        }
    }
}