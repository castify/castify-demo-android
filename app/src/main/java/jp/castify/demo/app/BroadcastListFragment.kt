package jp.castify.demo.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager

import jp.castify.demo.R
import jp.castify.demo.app
import jp.castify.demo.core.load
import kotlinx.android.synthetic.main.fragment_broadcast_list.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BroadcastListFragment : Fragment(), BroadcastListAdapter.Callback {

  private val listAdapter by lazy {
    BroadcastListAdapter(requireContext().app.castifyApp.config,this)
  }

  private var refreshingJob: Job? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    inflater.inflate(R.layout.fragment_broadcast_list, container, false)

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    broadcastList.setHasFixedSize(true)
    broadcastList.adapter = listAdapter
    broadcastList.layoutManager = GridLayoutManager(context, BroadcastListAdapter.SPAN_COUNT).also {
      it.orientation    = GridLayoutManager.VERTICAL
      it.spanSizeLookup = listAdapter.spanSizeLookup
    }

    broadcastButton.setOnClickListener {
      findNavController().navigate(BroadcastListFragmentDirections.actionBroadcastListFragmentToBroadcastFragment())
    }

    broadcastListRefresher.setOnRefreshListener {
      reloadList()
    }
  }

  override fun onResume() {
    super.onResume()
    reloadList()
  }

  override fun onPause() {
    super.onPause()
    refreshingJob?.cancel()
    refreshingJob = null
  }

  private fun reloadList() {
    if (refreshingJob != null) {
      return
    }
    refreshingJob = lifecycleScope.launch {
      try {
        val page = requireContext()
          .app.castifyApi
          .listBroadcast()
          .load()

        val (lives, archives) = page.items
          .map(BroadcastListAdapter.Item::Column)
          .partition {
            it.data.stoppedAt == null
          }

        listAdapter.reset(mutableListOf<BroadcastListAdapter.Item>().apply {
          if (lives.isNotEmpty()) {
            add(BroadcastListAdapter.Item.Header("Lives"))
            addAll(lives)
          }
          if (archives.isNotEmpty()) {
            add(BroadcastListAdapter.Item.Header("Archives"))
            addAll(archives)
          }
        })
      }
      finally {
        broadcastListRefresher.isRefreshing = false
      }
    }
  }

  override fun onColumnClick(item: BroadcastListAdapter.Item.Column) {
    findNavController().navigate(BroadcastListFragmentDirections.actionBroadcastListFragmentToPlayerFragment(item.data.broadcastId))
  }
}
