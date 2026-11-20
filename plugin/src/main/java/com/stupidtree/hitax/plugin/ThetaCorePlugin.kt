package com.stupidtree.hitax.plugin

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.HostEnvironmentService
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.ThetaArticle
import com.stupidtree.hita.api.ThetaService
import com.stupidtree.hita.api.ThetaTopic
import com.stupidtree.hita.theta.data.repository.TopicRepository
import com.stupidtree.hita.theta.data.source.web.ArticleWebSource

/** T1 theta topic core. Article/comment data remains in the legacy module. */
class ThetaCorePlugin : Plugin {
    override val id = "com.stupidtree.hitax.theta"
    override val displayName = "社区话题核心"
    override val requiresApi = 11

    private var application: Application? = null

    override fun install(ctx: PluginContext) {
        val app = (ctx.service(HostEnvironmentService.KEY)?.applicationContext as? Application)
            ?: error("缺少宿主 applicationContext")
        application = app
        ctx.provide(ThetaService.KEY, ThetaImpl(app))
    }

    override fun purgeData() = Unit
}

private class ThetaImpl(application: Application) : ThetaService {
    private val repo = TopicRepository(application)
    override fun topics(mode: String, page: Int): LiveData<DataState<List<ThetaTopic>>> =
        repo.getTopics("", mode, 20, page, "").map { state ->
            if (state.state != DataState.STATE.SUCCESS) DataState<List<ThetaTopic>>(state.state).apply { message = state.message }
            else DataState(state.data.orEmpty().map { ThetaTopic(it.id, it.name, it.description, it.avatar, it.articleNum) }, DataState.STATE.SUCCESS)
        }

    private val articleSource = ArticleWebSource.getInstance(application)
    override fun articles(topicId: String, page: Int): LiveData<DataState<List<ThetaArticle>>> =
        articleSource.getArticles("", "new", 0, 0, 20, topicId).map { state ->
            if (state.state != DataState.STATE.SUCCESS) DataState<List<ThetaArticle>>(state.state).apply { message = state.message }
            else DataState(state.data.orEmpty().map { ThetaArticle(it.id, it.topicName ?: "", it.content, it.authorName, it.createTime.time) }, DataState.STATE.SUCCESS)
        }
}
