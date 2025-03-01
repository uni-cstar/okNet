package unics.oknet

import android.app.Application
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.LoggingEventListener
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import unics.oknet.OkNet.setup
import unics.oknet.okhttp.OkDomain
import unics.oknet.okhttp.OnConflictStrategy
import unics.oknet.request.ProgressInterceptor
import java.util.concurrent.TimeUnit

/**
 * 简易的网络api请求客户端；也可以单独使用[OkDomain]的功能
 * 使用前请先调用[setup]方法进行初始化配置，支持懒加载
 * */
object OkNet {

    /**
     * 延迟初始化
     */
    interface LazyInitializer {

        val app: Application

        /**
         * 初始主地址，在初始化后会将该地址配置为mainBaseUrl
         */
        val baseUrl: String

        /**
         * 配置：用于配置用户的自定义配置
         * 注意：不用再调用[addOkDomain]放，内部已经调用了这个方法
         */
        fun onSetup(oBuilder: OkHttpClient.Builder, rBuilder: Retrofit.Builder)

        /**
         * 配置完成
         */
        fun onConfigured() {}

    }

    private lateinit var mOkHttpClient: OkHttpClient
    private lateinit var mRetrofit: Retrofit
    private val mApiServiceCaches = mutableMapOf<Class<*>, Any>()
    private var mInit: Boolean = false
    private var mLazyInitializer: LazyInitializer? = null
    private lateinit var mApp: Application

    internal var DEBUG = OkDomain.debuggable

    @JvmStatic
    val app: Application
        get() {
            if (mInit)
                return mApp
            //如果没有初始化，则使用的是懒加载
            return mLazyInitializer!!.app
        }

    /**
     * 设置是否允许调试
     */
    @JvmStatic
    fun setDebug(debuggable: Boolean) {
        OkDomain.debuggable = debuggable
        DEBUG = debuggable
    }

    /**
     * 获取OkHttp客户端实例，必须先调用[setup]方法，否则会抛出异常
     */
    @JvmStatic
    val okHttpClient: OkHttpClient
        get() {
            requirePerformLazyInit()
            return mOkHttpClient
        }

    /**
     * 快速配置：用于通常没有自定义配置，开箱即用
     * @param baseUrl 基地址
     * @param debug 是否开启调试日志
     * @param connectTimeoutMsec 链接超时，默认30000
     * @param readTimeoutMsec 读取超时，默认30000
     * @param retryOnConnectionFailure  连接失败是否重连，默认true
     * @param converterFactory 用于retrofit配置的json工厂
     * @param mainHeaders 主域名配置的全局header，后续可以修改；[Triple.first]-对应header的key，[Triple.second]-对应header的value，[Triple.third]-对应header冲突处理策略
     */
    @JvmStatic
    fun setup(
        app: Application,
        baseUrl: String,
        debug: Boolean = false,
        connectTimeoutMsec: Long = 30 * 1000,
        readTimeoutMsec: Long = 30 * 1000,
        retryOnConnectionFailure: Boolean = true,
        converterFactory: Converter.Factory? = quicklyPreferredConverterFactory(),
        vararg mainHeaders: Triple<String, String, OnConflictStrategy>
    ) {
        setup(
            quicklyLazyInitializer(
                app,
                baseUrl,
                debug,
                connectTimeoutMsec,
                readTimeoutMsec,
                retryOnConnectionFailure,
                converterFactory,
                *mainHeaders
            )
        )
    }

    /**
     * 设置懒加载
     */
    @JvmStatic
    fun setup(lazyInitializer: LazyInitializer) {
        require(!mInit) {
            "OkNetClient has already been configured and cannot be configured repeatedly"
        }
        mLazyInitializer = lazyInitializer
    }

    @JvmOverloads
    @JvmStatic
    fun setup(
        app: Application,
        baseUrl: String,
        initializer: (OkHttpClient.Builder, Retrofit.Builder) -> Unit = { obuilder, rbuilder -> }
    ) {
        performSetup(app, baseUrl, initializer)
    }

    /**
     * 确保懒加载已执行
     */
    private fun requirePerformLazyInit() {
        if (mInit) {
            return
        }
        synchronized(this) {
            if (mInit) {
                return
            }
            val lazyInitializer =
                mLazyInitializer ?: throw RuntimeException("please call setup method first.")
            performSetup(lazyInitializer.app, lazyInitializer.baseUrl, lazyInitializer::onSetup)
            lazyInitializer.onConfigured()
            mLazyInitializer = null
        }
    }

    private fun performSetup(
        app: Application,
        baseUrl: String,
        initializer: (OkHttpClient.Builder, Retrofit.Builder) -> Unit
    ) {
        synchronized(this) {
            require(!mInit) {
                "OkNetClient has already been configured and cannot be configured repeatedly"
            }
            require(baseUrl.isNotBlank()) {
                "baseUrl must not be empty."
            }
            mApp = app
            val oBuilder = OkHttpClient.Builder()
                .addOkDomain(baseUrl)
                .addInterceptor(ProgressInterceptor())
            val rBuilder = Retrofit.Builder()
            rBuilder.baseUrl(baseUrl)
            initializer.invoke(oBuilder, rBuilder)
            mOkHttpClient = oBuilder.build()
            mRetrofit = rBuilder.client(mOkHttpClient).build()
            mInit = true
        }
    }

    private fun quicklyPreferredConverterFactory(): Converter.Factory? {
        return if (isDependOn("retrofit2.converter.moshi.MoshiConverterFactory")) {
            MoshiConverterFactory.create()
        } else if (isDependOn("retrofit2.converter.gson.GsonConverterFactory")) {
            GsonConverterFactory.create()
        } else {
//            throw RuntimeException("can not found moshi and gson converter factory,please specified converter directly.")
            logd {
                "can not found moshi and gson converter factory,please specified converter directly."
            }
            null

        }
    }

    /**
     * 根据配置创建懒加载器
     * @param connectTimeoutMsec 链接超时，默认30000
     * @param readTimeoutMsec 读取超时，默认30000
     * @param retryOnConnectionFailure  连接失败是否重连，默认true
     */
    private fun quicklyLazyInitializer(
        app: Application,
        baseUrl: String,
        debug: Boolean = false,
        connectTimeoutMsec: Long = 5 * 1000,
        readTimeoutMsec: Long = 15 * 1000,
        retryOnConnectionFailure: Boolean = true,
        converterFactory: Converter.Factory? = quicklyPreferredConverterFactory(),
        vararg mainHeaders: Triple<String, String, OnConflictStrategy>
    ): LazyInitializer {

        return object : LazyInitializer {

            override val app: Application = app

            override val baseUrl: String = baseUrl

            override fun onSetup(oBuilder: OkHttpClient.Builder, rBuilder: Retrofit.Builder) {
                setDebug(debug)
                oBuilder.connectTimeout(connectTimeoutMsec, TimeUnit.MILLISECONDS)
                    .readTimeout(readTimeoutMsec, TimeUnit.MILLISECONDS)
                    .retryOnConnectionFailure(retryOnConnectionFailure) //
                ////指定TLS
                //trustSpecificCertificate(App.instance,"ucuxin7434801.pem",builder)
                if (debug) {
                    if (isDependOn("okhttp3.logging.HttpLoggingInterceptor")) {
                        //logging 拦截器，okhttp.logging提供，主要是用于输出网络请求和结果的Log
                        val httpLoggingInterceptor = HttpLoggingInterceptor()
                        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY//配置输出级别
                        oBuilder.addInterceptor(httpLoggingInterceptor)//配置日志拦截器
                        oBuilder.eventListenerFactory(LoggingEventListener.Factory())//配置完整logging事件
                    }
                }

                rBuilder.baseUrl(baseUrl)   //配置服务器路径
                if (converterFactory != null) //配置converter
                    rBuilder.addConverterFactory(converterFactory)
            }

            override fun onConfigured() {
                super.onConfigured()
                mainHeaders.forEach { addMainHeader(it.first, it.second, it.third) }
            }
        }
    }

    /**
     * 修改主域名
     */
    @JvmStatic
    fun setMainDomain(url: String) {
        requirePerformLazyInit()
        OkDomain.setMainDomain(url)
    }

    /**
     * set the domain of the specified name.
     *
     * 设置域名为[name]的[url]地址,通常是配置其他域名
     *
     * @param name 域名的key，标识符，比如使用腾讯的域名，那么自定义一个标识符区别该域名 ,比如使用tencent
     */
    @JvmStatic
    fun setDomain(name: String, url: String) {
        requirePerformLazyInit()
        OkDomain.setDomain(name, url)
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
        requirePerformLazyInit()
        OkDomain.addMainHeader(key, value, conflictStrategy)
    }

    /**
     * Removes the specified key and its corresponding value from the global headers of the Main Domain.
     * 从主域名的全局header配置中移除指定key的配置
     * @return the previous value associated with the key, or null if the key was not present in the global header.
     */
    @JvmStatic
    fun removeMainHeader(key: String): Pair<String, OnConflictStrategy>? {
        requirePerformLazyInit()
        return OkDomain.removeMainHeader(key)
    }

    @JvmStatic
    @JvmOverloads
    fun addHeader(
        domainName: String,
        key: String,
        value: String,
        conflictStrategy: OnConflictStrategy = OnConflictStrategy.IGNORE
    ) {
        requirePerformLazyInit()
        OkDomain.addHeader(domainName, key, value, conflictStrategy)
    }

    @JvmStatic
    fun removeHeader(domainName: String, key: String): Pair<String, OnConflictStrategy>? {
        requirePerformLazyInit()
        return OkDomain.removeHeader(domainName, key)
    }

    /**
     * 创建ApiService
     * @param cacheable 是否使用缓存：建议反复、长期使用的ApiService可以全局保存
     */
    @JvmStatic
    fun <T : Any> createApiService(clz: Class<T>, cacheable: Boolean): T {
        requirePerformLazyInit()
        return if (cacheable) {
            @Suppress("UNCHECKED_CAST")
            mApiServiceCaches.getOrPut(clz) {
                mRetrofit.create(clz)
            } as T
        } else {
            mRetrofit.create(clz)
        }
    }

    /**
     * 移除指定的ApiService
     */
    @JvmStatic
    fun removeApiService(clz: Class<*>): Any? {
        return mApiServiceCaches.remove(clz)
    }

    /**
     * 清空所有的ApiService缓存
     */
    @JvmStatic
    fun clearApiService() {
        mApiServiceCaches.clear()
    }

}