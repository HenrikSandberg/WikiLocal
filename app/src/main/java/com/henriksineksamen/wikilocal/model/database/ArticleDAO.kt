package com.henriksineksamen.wikilocal.model.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ArticleDAO {
    @Insert fun insert(vararg article: Article)
    @Update fun update (vararg article: Article)
    @Delete fun delete(article: Article)

    @Query("DELETE FROM article_table")
    fun deleteAll()

    @Query("SELECT * FROM article_table")
    fun getAllArticlesLive() : LiveData<List<Article>>

    @Query("SELECT * FROM article_table")
    fun getAllArticles() : List<Article>
}