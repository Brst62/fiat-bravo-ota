package com.nwd.fiatlauncher.ui

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.nwd.fiatlauncher.R
import com.nwd.fiatlauncher.databinding.ActivityFiatLauncherBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FiatLauncherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFiatLauncherBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFiatLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupGrid()
    }

    private fun setupGrid() {
        val tiles = listOf(
            Tile("Dashboard",   "com.nwd.feature.dashboard"),
            Tile("Radyo",       "com.nwd.feature.radio"),
            Tile("OBD2",        "com.nwd.core.obd2"),
            Tile("Sürüş Raporu","com.nwd.feature.drivingreport"),
            Tile("Bakım",       "com.nwd.feature.maintenance"),
            Tile("Ayarlar",     "com.nwd.feature.settings"),
            Tile("Tema",        "com.nwd.feature.thememanager"),
            Tile("OTA",         "com.nwd.ota.github"),
            // Externalapp olarak ayrı APK'lar:
            Tile("Kamera",      "com.nwd.smartcamera",  external = true),
            Tile("Acil Durum",  "com.nwd.emergency",    external = true),
            Tile("Sesli Asistan","com.nwd.voice",       external = true),
        )
        binding.tileGrid.layoutManager = GridLayoutManager(this, 4)
        binding.tileGrid.adapter = TileAdapter(tiles) { tile ->
            launchTile(tile)
        }
    }

    private fun launchTile(tile: Tile) {
        if (tile.external) {
            packageManager.getLaunchIntentForPackage(tile.pkg)?.let { startActivity(it) }
            return
        }
        // in-process modüller: ileride router ile fragment swap. Şimdilik no-op.
    }

    data class Tile(val label: String, val pkg: String, val external: Boolean = false)
}
