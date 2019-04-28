package com.henriksineksamen.wikilocal.model
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.henriksineksamen.wikilocal.R
import com.henriksineksamen.wikilocal.controller.NearYouFragment.OnNearYouFragmentInteractionListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_near_you_content.view.*
import org.json.JSONObject

class NearYouRecyclerViewAdapter(
    private val list:  MutableList<JSONObject>,
    private val nearYouListener: OnNearYouFragmentInteractionListener?
) : RecyclerView.Adapter<NearYouRecyclerViewAdapter.ViewHolder>() {
    private val onClickListenerForView: View.OnClickListener

    init {
        onClickListenerForView = View.OnClickListener { view ->
            val article = view.tag as JSONObject
            nearYouListener?.onNearYouFragmentInteraction(article)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_near_you_content, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = list[position]

        if (article.has("originalimage")){
            Picasso.with(holder.mView.context)
                .load(article
                    .getJSONObject("originalimage")
                    .getString("source"))
                .into(holder.imageView)
        } else {
            holder.imageView.setImageResource(R.drawable.no_image_available)
        }

        holder.title.text = if (article.has("displaytitle")){
             article.getString("displaytitle")
        } else {
            "No title available"
        }

        holder.description.text = when {
            article.has("description") -> article.getString("description")
            article.has("extract") -> article.getString("extract")
            else -> "No description available"
        }

        with(holder.mView) {
            tag = article
            setOnClickListener(onClickListenerForView)
        }
    }

    override fun getItemCount(): Int = list.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val imageView: ImageView = mView.thumbnail
        val description: TextView = mView.item_number
        val title: TextView = mView.content

        override fun toString(): String = super.toString() + " '" + title.text + "'"
    }
}
