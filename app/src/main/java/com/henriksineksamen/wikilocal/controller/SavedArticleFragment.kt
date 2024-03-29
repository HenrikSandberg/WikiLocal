package com.henriksineksamen.wikilocal.controller

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.henriksineksamen.wikilocal.R
import com.henriksineksamen.wikilocal.model.SavedArticleRecyclerViewAdapter
import com.henriksineksamen.wikilocal.model.database.Article
import com.henriksineksamen.wikilocal.model.database.ArticleModel
class SavedArticleFragment : Fragment() {
    private var listener: OnListFragmentInteractionListener? = null

    override fun onCreateView (
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_saved_article_list, container, false)
        val articleModel = ViewModelProviders.of(this).get(ArticleModel::class.java)

        if (view is RecyclerView) {
            articleModel.allArticles.observe(this, Observer { articles ->
                with (view) {
                    layoutManager = LinearLayoutManager(context)
                    adapter = SavedArticleRecyclerViewAdapter(articles.reversed(), listener)
                }
            })
        }
        return view
    }

    override fun onAttach (context: Context) {
        super.onAttach (context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnListFragmentInteractionListener {
        fun onSavedArticleFragmentInteraction(article: Article)
    }
}
