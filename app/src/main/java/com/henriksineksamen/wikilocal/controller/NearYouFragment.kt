package com.henriksineksamen.wikilocal.controller

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.henriksineksamen.wikilocal.R
import com.henriksineksamen.wikilocal.model.NearYouRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_near_you_list.view.*
import org.json.JSONObject

class NearYouFragment : Fragment() {
    private var listener: OnNearYouFragmentInteractionListener? = null
    private var articles: MutableList<JSONObject> = mutableListOf()
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeContainer: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_near_you_list, container, false)
        recyclerView = view.recycler

        swipeContainer = view.swipeContainer
        swipeContainer.setOnRefreshListener {
            updateWithThread()
            refersRecyclerView()
        }
        swipeContainer.setColorSchemeResources (
            R.color.color_primary,
            R.color.spin_first_color,
            R.color.spin_second_color,
            R.color.spin_third_color,
            R.color.spin_fourth_color
        )
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnNearYouFragmentInteractionListener) listener = context
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    fun updateList (articles: MutableList<JSONObject>) { this.articles = articles }

    fun refersRecyclerView() {
        with (recyclerView) {
            layoutManager = LinearLayoutManager(context)
            adapter = NearYouRecyclerViewAdapter(articles, listener)
        }
    }

    private fun updateWithThread() {
        (activity as MainActivity).requestArticles(null)
    }

    fun removeUpdaterIcon() {
        swipeContainer.isRefreshing = false
    }

    interface OnNearYouFragmentInteractionListener {
        fun onNearYouFragmentInteraction(article: JSONObject)
    }
}
