package jp.castify.demo.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import jp.castify.api.CastifyAppConfig
import jp.castify.demo.R
import jp.castify.demo.core.Broadcast
import kotlinx.android.synthetic.main.broadcast_list_column.view.*
import java.lang.IllegalArgumentException

class BroadcastListAdapter(private val config: CastifyAppConfig, var callback: Callback? = null) : RecyclerView.Adapter<BroadcastListAdapter.Holder>() {

  companion object {
    const val SPAN_COUNT = 2
  }

  interface Callback {
    fun onColumnClick(item: Item.Column)
  }

  val spanSizeLookup = object: GridLayoutManager.SpanSizeLookup() {

    override fun getSpanSize(position: Int): Int = items[position].spanSize
  }

  sealed class Item {

    companion object {
      const val HEADER = 1
      const val COLUMN = 2
    }

    abstract val viewType: Int
    abstract val spanSize: Int

    data class Header(val title: String) : Item() {
      override val viewType: Int get() = HEADER
      override val spanSize: Int get() = 2
    }

    data class Column(val data: Broadcast) : Item() {
      override val viewType: Int get() = COLUMN
      override val spanSize: Int get() = 1
    }
  }


  class Holder(view: View) : RecyclerView.ViewHolder(view)

  private var items = listOf<Item>()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
    Holder(when (viewType) {
      Item.HEADER ->
        LayoutInflater
          .from(parent.context)
          .inflate(R.layout.broadcast_list_header, parent, false)

      Item.COLUMN ->
        LayoutInflater
          .from(parent.context)
          .inflate(R.layout.broadcast_list_column, parent, false)

      else -> throw IllegalArgumentException()
    })

  override fun onBindViewHolder(holder: Holder, position: Int) {
    when (val e = items[position]) {
      is Item.Header -> {
        holder.itemView.title.text = e.title
      }
      is Item.Column -> {

        Picasso.get()
          .load(config.publicPreviewUrl(e.data.broadcastId))
          .into(holder.itemView.image)

        holder.itemView
          .title
          .text = e.data.broadcastId

        holder.itemView
          .status
          .let {
            if (e.data.stoppedAt == null) {
              it.text = "ON AIR"
              it.setBackgroundResource(android.R.color.holo_red_light)
            }
            else {
              it.text = "ARCHIVE"
              it.setBackgroundResource(android.R.color.holo_blue_dark)
            }
          }

        holder.itemView.setOnClickListener { callback?.onColumnClick(e) }
      }
    }
  }

  override fun getItemCount() = items.size

  override fun getItemViewType(position: Int): Int = items[position].viewType

  fun reset(src: List<Item>) {
    items = src
    notifyDataSetChanged()
  }
}
