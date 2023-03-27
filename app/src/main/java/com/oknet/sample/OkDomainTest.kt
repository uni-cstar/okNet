package com.oknet.sample

import libcore.net.okhttp.OkDomain.DOMAIN_NAME_HEADER
import retrofit2.http.*

interface OkDomainTest {

    /**
     * 请求必应搜索（模拟常规，base url只包含域名），base url = https://www.bing.com/
     * https://www.bing.com/search?q=retrofit%E5%A6%82%E4%BD%95
     */
    @GET("search")
    suspend fun requestBingSearch(@Query("q") q: String): String

    /**
     * 请求掘金（模拟常规，baseurl带了segment），base url = https://juejin.cn/post/
     * https://juejin.cn/post/6891269497450135560
     *
     */
    @GET("sdfsd/{id}")
    @Headers(DOMAIN_NAME_HEADER + "domain1")
    suspend fun requestSeq(@Path("id") q: String = "6891269497450135560"): String

    /**
     * https://www.kancloud.cn/alex_wsc/androids/473787
     */
    @GET("{id}")
    @Headers(DOMAIN_NAME_HEADER + "domain2")
    suspend fun requestSeq2(@Path("id") q: String = "473787"): String

    /**
     * 模拟请求，直接使用url请求
     */
    @GET
    suspend fun requestUrl(@Url url: String = "https://item.jd.com/100009464799.html"): String


    /**
     * 请求必应搜索（模拟常规，base url只包含域名），base url = https://www.bing.com/
     * https://www.bing.com/search?q=retrofit%E5%A6%82%E4%BD%95
     */
    @GET("search")
    suspend fun testHeader(@Query("q") q: String): String

    @Headers("$TEST_HEADER:ServiceHeaderValue")
    @GET("search")
    suspend fun testHeaderIgnore(@Query("q") q: String): String

    @Headers(
        DOMAIN_NAME_HEADER + "domain1",
        "$TEST_HEADER:ServiceHeaderValue"
    )
    @GET("sdfsd/{id}")
    suspend fun testHeaderReplace(@Query("q") q: String): String

    @GET("{id}")
    @Headers(
        DOMAIN_NAME_HEADER + "domain2",
        "$TEST_HEADER:ServiceHeaderValue"
    )
    suspend fun testHeaderAdd(@Path("id") q: String = "473787"): String

    @Headers("abort_key:ServiceHeaderValue")
    @GET("search/q=ss")
    suspend fun testHeaderAbort(@Header("abort_key")abortKey:String): String
}