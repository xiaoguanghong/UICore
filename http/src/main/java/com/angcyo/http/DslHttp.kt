package com.angcyo.http

import com.angcyo.http.DslHttp.dslHttpConfig
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.*

/**
 * 网络请求库
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/12/25
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

interface Api {

    /*------------以下是[POST]请求-----------------*/

    /**Content-Type: application/json;charset=UTF-8*/
    @POST
    fun post(
        @Url url: String,
        @Body json: JsonElement = JsonObject(),
        @QueryMap queryMap: HashMap<String, Any> = hashMapOf()
    ): Observable<Response<JsonElement>>

    /*------------以下是[GET]请求-----------------*/

    /**Content-Type: application/json;charset=UTF-8*/
    @GET
    fun get(
        @Url url: String,
        @QueryMap queryMap: HashMap<String, Any> = hashMapOf()
    ): Observable<Response<JsonElement>>

}

object DslHttp {
    val dslHttpConfig = DslHttpConfig()

    /**自定义配置, 否则使用库中默认配置*/
    fun config(action: DslHttpConfig.() -> Unit) {
        dslHttpConfig.action()
    }
}

/**
 * 通用接口请求
 * */
fun dslHttp(): Api {

    val baseUrl = dslHttpConfig.onGetBaseUrl()

    if (baseUrl.isEmpty()) {
        throw NullPointerException("请先初始化[DslHttp.config{ ... }]")
    }

    val client = dslHttpConfig.onBuildHttpClient(dslHttpConfig.defaultOkHttpClientBuilder)
    dslHttpConfig.okHttpClient = client

    val retrofit = dslHttpConfig.onBuildRetrofit(dslHttpConfig.defaultRetrofitBuilder, client)
    dslHttpConfig.retrofit = retrofit

    /*如果单例API对象的话, 就需要在动态切换BaseUrl的时候, 重新创建. 否则不会生效*/
    return retrofit.create(Api::class.java)
}

/**拼接 host 和 api接口*/
fun connectUrl(host: String?, url: String?): String {
    val h = host?.trimEnd('/') ?: ""
    val u = url?.trimStart('/') ?: ""
    return "$h/$u"
}

fun http(config: RequestConfig.() -> Unit): Observable<Response<JsonElement>> {
    val requestConfig = RequestConfig(GET)
    requestConfig.config()

    if (requestConfig.autoConnectUrl && !requestConfig.url.startsWith("http")) {
        requestConfig.url =
            connectUrl(dslHttpConfig.onGetBaseUrl(), requestConfig.url)
    }

    return if (requestConfig.method == POST) {
        dslHttp().post(requestConfig.url, requestConfig.body, requestConfig.query)
    } else {
        dslHttp().get(requestConfig.url, requestConfig.query)
    }
}

/**快速发送一个[get]请求*/
fun get(config: RequestConfig.() -> Unit): Observable<Response<JsonElement>> {
    return http(config)
}

/**快速发送一个[post]请求*/
fun post(config: RequestConfig.() -> Unit): Observable<Response<JsonElement>> {
    return http {
        method = POST
        this.config()
    }
}

const val GET = 1
const val POST = 2

data class RequestConfig(
    //请求方法
    var method: Int = GET,
    //接口api, 可以是全路径, 也可以是相对于baseUrl的路径
    var url: String = "",
    //自动根据url不是http开头,拼接上baseUrl
    var autoConnectUrl: Boolean = true,
    //body数据, 仅用于post请求
    var body: JsonElement = JsonObject(),
    //url后面拼接的参数列表
    var query: HashMap<String, Any> = hashMapOf(),

    var onStart: () -> Unit = {},
    var onSuccess: () -> Unit = {},
    var onError: () -> Unit = {}
)

