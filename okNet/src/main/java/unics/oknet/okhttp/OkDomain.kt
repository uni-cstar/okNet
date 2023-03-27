/*
 * Copyright  2023 ,luochao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package unics.oknet.okhttp

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import unics.oknet.logd
import unics.oknet.okhttp.OkDomain.addHeader
import unics.oknet.okhttp.OkDomain.addMainHeader
import unics.oknet.okhttp.OkDomain.enable
import unics.oknet.okhttp.OkDomain.removeHeader
import unics.oknet.okhttp.OkDomain.removeMainHeader
import unics.oknet.okhttp.OkDomain.setDomain
import unics.oknet.okhttp.OkDomain.setMainDomain
import unics.oknet.okhttp.OkDomain.useOkDomain

/**
 * 用于动态配置和切换Okhttp的baseurl
 * 具体用法查看README文档
 * create by luochao at 2023/3/24
 *
 * @see useOkDomain 使用该方法为[OkHttpClient.Builder]绑定功能支持
 *
 *
 * @see enable 启用/禁用
 * @see setMainDomain set the main domain
 * @see setDomain set the domain of the specified name
 * @see addMainHeader set the global headers of the main domain
 * @see removeMainHeader remove the global header of the main domain by the key
 * @see addHeader add the header corresponding to the domain by the key
 * @see removeHeader remove the header corresponding to the domain by the key
 */
object OkDomain {

    const val DOMAIN_NAME = "Domain-Name"
    const val DOMAIN_NAME_HEADER = "$DOMAIN_NAME:"

    private var domainInterceptor: DomainInterceptor? = null

    var enable: Boolean = true
        @JvmStatic
        set(value) {
            field = value
            domainInterceptor?.enable = value
        }

    /**
     * 是否开启调试日志
     */
    @get:JvmStatic
    @set:JvmStatic
    var debuggable: Boolean = false

    /**
     * 使用OkDomain
     */
    @JvmStatic
    fun useOkDomain(builder: OkHttpClient.Builder, baseUrl: String) {
        val interceptor = domainInterceptor
        if (interceptor != null && builder.interceptors().contains(interceptor)) {
            return
        }
        builder.addInterceptor(interceptor ?: DomainInterceptor(baseUrl).also {
            domainInterceptor = it
        })
    }

    /**
     * 设置主域名
     */
    @JvmStatic
    fun setMainDomain(url: String) {
        val interceptor = domainInterceptor
            ?: throw RuntimeException("set main domain require call method ${OkDomain::useOkDomain.name} first.")
        interceptor.setMainDomain(url)
    }

    /**
     * set the domain of the specified name.
     * 设置[name]表示的域名为[url],通常是配置其他域名；在定义接口时通过在Header中添加如下格式"Domain-Name:[name]"的内容值即可绑定该域名；
     * 为避免编写出错，也可以使用[DOMAIN_NAME_HEADER]拼接[name]添加到header中；
     * 假如name=baidu，则最终在定义的retrofit上添加的header内容为 @Headers(DOMAIN_NAME_HEADER + "baidu")
     * @param name 域名的key，标识符，比如使用腾讯的域名，那么自定义一个标识符区别该域名 ,比如使用tencent
     *
     */
    @JvmStatic
    fun setDomain(name: String, url: String) {
        val interceptor = domainInterceptor
            ?: throw RuntimeException("set domain require call method ${OkDomain::useOkDomain.name} first.")
        interceptor.setDomain(name, url)
    }

    /**
     * set the global headers of the main domain
     * @param key the key of main domain header.
     * @param value the value of main domain header
     */
    @JvmStatic
    @JvmOverloads
    fun addMainHeader(
        key: String,
        value: String,
        conflictStrategy: OnConflictStrategy = OnConflictStrategy.IGNORE
    ) {
        val interceptor = domainInterceptor
            ?: throw RuntimeException("set domain require call method ${OkDomain::useOkDomain.name} first.")
        interceptor.addMainHeader(key, value, conflictStrategy)
    }

    /**
     * Removes the specified key and its corresponding value from the global headers of the Main Domain.
     * 从主域名的全局header配置中移除指定key的配置
     * @return the previous value associated with the key, or null if the key was not present in the global header.
     */
    @JvmStatic
    fun removeMainHeader(key: String): Pair<String, OnConflictStrategy>? {
        return domainInterceptor?.removeMainHeader(key)
    }

    @JvmStatic
    @JvmOverloads
    fun addHeader(
        domainName: String,
        key: String,
        value: String,
        conflictStrategy: OnConflictStrategy = OnConflictStrategy.IGNORE
    ) {
        val interceptor = domainInterceptor
            ?: throw RuntimeException("set domain require call method ${OkDomain::useOkDomain.name} first.")
        interceptor.addHeader(domainName, key, value, conflictStrategy)
    }

    @JvmStatic
    fun removeHeader(domainName: String, key: String): Pair<String, OnConflictStrategy>? {
        return domainInterceptor?.removeHeader(domainName, key)
    }

    /**
     * 域名切换以及域名对应的全局Header 拦截器
     * @param baseUrl 主域名，必须设置，后面可以修改，不能为空
     * @see enable 启用/禁用
     * @see setMainDomain 设置主域名
     * @see setDomain 设置其他域名
     * @see addMainHeader
     * @see removeMainHeader
     * @see addHeader
     * @see removeHeader
     */
    internal class DomainInterceptor(baseUrl: String) : Interceptor {

        @JvmField
        var enable: Boolean = true

        private val configs = mutableMapOf<String, DomainConfig>()

        /**
         * 设置主域名
         */
        fun setMainDomain(url: String) = setDomain(MAIN_DOMAIN, url)

        /**
         * 设置[name]表示的域名为[url],通常是配置其他域名
         * @param name 域名的key，标识符，比如使用腾讯的域名，那么自定义一个标识符区别该域名 ,比如使用tencent
         */
        fun setDomain(
            name: String,
            url: String
        ) {
            val cache = configs[name]
            if (cache != null) {
                logd {
                    "[DomainInterceptor#setDomain] the domain config (key=$name) is exists,do update"
                }
                val previous = cache.expectBaseUrl
                cache.updateBaseUrl(url)
                //从老的中移除目标地址
                cache.oldBaseUrls.remove(url)
                logd { "[DomainInterceptor#setDomain] set previous ($previous) to expect (${cache.expectBaseUrl})" }
            } else {
                configs[name] = DomainConfig(name, url)
                logd { "[DomainInterceptor#setDomain] save the new domain config (key=$name,url=$url)" }
            }
        }

        fun addMainHeader(
            key: String, value: String,
            conflictStrategy: OnConflictStrategy = OnConflictStrategy.IGNORE
        ) = addHeader(MAIN_DOMAIN, key, value, conflictStrategy)


        fun removeMainHeader(key: String): Pair<String, OnConflictStrategy>? =
            removeHeader(MAIN_DOMAIN, key)

        fun addHeader(
            domainName: String, key: String, value: String,
            conflictStrategy: OnConflictStrategy = OnConflictStrategy.IGNORE
        ): Pair<String, OnConflictStrategy>? {
            val cache = configs[domainName]
            require(cache != null) {
                "[DomainInterceptor#addHeader] the domain config named '$domainName' not found,please use call ${::setDomain} method before add header."
            }
            logd { "[DomainInterceptor#addHeader] add header to the domain config named '$domainName' (key=${key},value=$value,conflictStrategy=$conflictStrategy)" }
            return cache.addHeader(key, value, conflictStrategy)
        }

        fun removeHeader(domainName: String, key: String): Pair<String, OnConflictStrategy>? {
            return configs[domainName]?.removeHeader(key)
        }

        override fun intercept(chain: Interceptor.Chain): Response {
            logd { "[DomainInterceptor]intercept" }
            return chain.proceed(handleRequest(chain.request()))
        }

        private fun handleRequest(request: Request): Request {
            if (!enable)
                return request
            logd { "[DomainInterceptor#handleRequest] handleRequest" }
            val domainName = obtainDomainNameFromHeaders(request)
            return if (domainName.isNullOrEmpty()) {
                //没有配置domain的，都是使用主域名
                logd { "[DomainInterceptor#handleRequest] the request does not set domain name,use main domain to transform" }
                transformRequest(configs[MAIN_DOMAIN]!!, request)
            } else {
                val domainConfig = configs[domainName]
                require(domainConfig != null) {
                    "can not found the base url of the domain name(=${domainName}) ,please call setDomain($domainName,your base url) method set before use."
                }
                logd { "[DomainInterceptor#handleRequest] the domain config found ,begin transform." }
                transformRequest(domainConfig, request)
            }
        }

        /**
         * 转换请求
         */
        private fun transformRequest(domainConfig: DomainConfig, request: Request): Request {
            synchronized(domainConfig) {
                val urlValue = request.url().toString()
                val baseUrl = obtainBaseUrl(urlValue, domainConfig)
                if (baseUrl.isNullOrEmpty()) {
                    logd { "[DomainInterceptor#transformRequest] the base url is not found in config domains,ure original request(ignore base url transform and global header set)." }
                    return request
                }

                return newRequest(baseUrl, request, domainConfig)
            }
        }

        /**
         * @param baseUrl the base url of the [request].
         * @param request the original request.
         * @param config DomainConfig corresponding to [baseUrl]
         */
        private fun newRequest(baseUrl: String, request: Request, config: DomainConfig): Request {
            val isBaseUrlSame = baseUrl == config.expectBaseUrl
            if (isBaseUrlSame && config.headers.isEmpty()) {
                logd { "[DomainInterceptor#newRequest] the base url is same with current config,and the global header is empty,use the original request." }
                return request
            }

            val builder = request.newBuilder()
            if (!isBaseUrlSame) {
                val urlValue = request.url().toString()
                val newUrlValue = urlValue.replace(baseUrl, config.expectBaseUrl)
                logd { "[DomainInterceptor#transformRequest] transform success,new url is $newUrlValue (the original base url is :$baseUrl)" }
                builder.url(newUrlValue)
            }
            val originalHeaders = request.headers()
            config.headers.forEach { (key, valuePair) ->
                valuePair.second.apply(originalHeaders, builder, key, valuePair.first)
            }
            return builder.build()
        }

        /**
         * 提取BaseUrl
         * find the base url of the [urlValue] from the [domainConfig]
         */
        private fun obtainBaseUrl(urlValue: String, domainConfig: DomainConfig): String? {
            if (urlValue.startsWith(domainConfig.expectBaseUrl)) {
                logd { "[DomainInterceptor#obtainBaseUrl] the original request url is start with the expect base url,return directly." }
                return domainConfig.expectBaseUrl
            }
            val baseUrl = domainConfig.oldBaseUrls.findLast {
                urlValue.startsWith(it)
            }
            if (!baseUrl.isNullOrEmpty()) {
                //如果在当前url chain中查找到
                logd { "[DomainInterceptor#obtainBaseUrl] find the base url,return." }
                return baseUrl
            }

            if (domainConfig.domainName == MAIN_DOMAIN) {
                logd { "[DomainInterceptor#obtainBaseUrl] not find base url,return directly." }
                return null
            } else {
                logd { "[DomainInterceptor#obtainBaseUrl] not find base url,try found in main domain." }
                val mainChain = configs[MAIN_DOMAIN]
                if (mainChain == null) {
                    logd { "[DomainInterceptor#obtainBaseUrl] main domain is null,return directly." }
                    return null
                }
                return obtainBaseUrl(urlValue, mainChain)
            }
        }

        /**
         * 从header中获取域名
         * obtain header form the request headers.
         */
        private fun obtainDomainNameFromHeaders(request: Request): String? {
            val headers = request.headers(DOMAIN_NAME)
            if (headers.isNullOrEmpty())
                return null
            require(headers.size == 1) { "Only one $DOMAIN_NAME in the headers" }
            return request.header(DOMAIN_NAME)
        }

        init {
            require(baseUrl.isNotEmpty()) {
                "the base url must not be empty."
            }
            setMainDomain(baseUrl)
        }
    }

    /**
     * @param domainName the name of the request
     * @param baseUrl the current base url of the domain
     */
    internal class DomainConfig(
        @JvmField val domainName: String,
        private var baseUrl: String
    ) {

        internal val oldBaseUrls = mutableListOf<String>()

        internal val headers = mutableMapOf<String, Pair<String, OnConflictStrategy>>()

        val expectBaseUrl: String get() = baseUrl

        /**
         * update the current base url
         */
        @Synchronized
        fun updateBaseUrl(url: String) {
            if (url == baseUrl) {
                logd { "[UrlChain#update] the new base url is same with current base url,ignore set. current = $baseUrl expect = $url" }
                return
            }
            val previous = baseUrl
            baseUrl = url
            oldBaseUrls.add(previous)
        }

        /**
         * add global header
         */
        fun addHeader(
            key: String,
            value: String,
            conflictStrategy: OnConflictStrategy
        ): Pair<String, OnConflictStrategy>? {
            return headers.put(key, Pair(value, conflictStrategy))
        }

        fun removeHeader(key: String): Pair<String, OnConflictStrategy>? {
            return headers.remove(key)
        }
    }

    internal const val MAIN_DOMAIN = "_MAIN_"

}