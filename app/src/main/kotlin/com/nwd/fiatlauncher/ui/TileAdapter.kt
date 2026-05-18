package com.nwd.fiatlauncher.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nwd.fiatlauncher.databinding.ItemTileBinding

class TileAdapter(
    private val items: List<FiatLauncherActivity.Tile>,
    private val onClick: (FiatLauncherActivity.Tile) -> Unit
) : RecyclerView.Adapter<TileAdapter.VH>() {

    inner class VH(val b: ItemTileBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemTileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = items[position]
        holder.b.tileLabel.text = t.label
        holder.b.root.setOnClickListener { onClick(t) }
    }

    override fun getItemCount(): Int = items.size
}
