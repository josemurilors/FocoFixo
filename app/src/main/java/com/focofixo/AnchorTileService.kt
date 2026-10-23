package com.focofixo

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Atalho na barra de configurações rápidas para ligar/desligar a âncora com um toque. */
class AnchorTileService : TileService() {
    override fun onStartListening() = refresh()

    override fun onClick() {
        if (AnchorService.running) {
            AnchorService.stop(this)
            setState(Tile.STATE_INACTIVE) // `running` muda de forma assíncrona; reflete a intenção já
        } else if (Settings.canDrawOverlays(this)) {
            AnchorService.start(this)
            setState(Tile.STATE_ACTIVE)
        } else {
            openApp() // sem permissão de overlay: a tela principal explica e pede
        }
    }

    private fun setState(s: Int) { qsTile?.apply { state = s; updateTile() } }

    private fun refresh() = setState(if (AnchorService.running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE)

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION") startActivityAndCollapse(intent)
        }
    }
}
