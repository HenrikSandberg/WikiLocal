package com.henriksineksamen.wikilocal.model

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.henriksineksamen.wikilocal.R


import com.henriksineksamen.wikilocal.controller.SavedArticleFragment.OnListFragmentInteractionListener
import com.henriksineksamen.wikilocal.model.database.Article
import com.squareup.picasso.Picasso

import kotlinx.android.synthetic.main.fragment_saved_article.view.*

class SavedArticleRecyclerViewAdapter(
    private val articleList: List<Article>,
    private val savedArticleListener: OnListFragmentInteractionListener?
) : RecyclerView.Adapter<SavedArticleRecyclerViewAdapter.ViewHolder>() {
    private val onArticleClickListener: View.OnClickListener

    init {
        onArticleClickListener = View.OnClickListener {
            val item = it.tag as Article
            savedArticleListener?.onSavedArticleFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater
            .from(parent.context)
            .inflate(R.layout.fragment_saved_article, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = articleList[position]
        holder.title.text = article.title
        holder.description.text = article.description

        Picasso.with(holder.savedArticleView.context)
            .load(article.image)
            .into(holder.image)
        with(holder.savedArticleView) {
            tag = article
            setOnClickListener(onArticleClickListener)
        }
    }

    override fun getItemCount(): Int = articleList.size

    inner class ViewHolder(val savedArticleView: View) : RecyclerView.ViewHolder(savedArticleView) {
        val title: TextView = savedArticleView.item_number
        val description: TextView = savedArticleView.content
        val image: ImageView = savedArticleView.thumbnail
        override fun toString(): String = "${super.toString()} ${description.text}"
    }
}
