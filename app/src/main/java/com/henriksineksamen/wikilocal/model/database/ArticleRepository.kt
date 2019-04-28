package com.henriksineksamen.wikilocal.model.database

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData

class ArticleRepository(private val articleDAO: ArticleDAO) {
    val allArticlesLive: LiveData<List<Article>> = articleDAO.getAllArticlesLive()

    @WorkerThread
    fun insert(article: Article){
        articleDAO.insert(article)
    }

    @WorkerThread
    fun delete(article: Article){
        articleDAO.delete(article)
    }
}