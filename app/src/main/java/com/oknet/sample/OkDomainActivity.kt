package com.oknet.sample

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.oknet.sample.databinding.NetOkdomainActivityBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import unics.oknet.OkNet
import unics.oknet.addOkDomain
import unics.oknet.apiService
import unics.oknet.okhttp.OkDomain
import unics.oknet.okhttp.OnConflictStrategy

class OkDomainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OkNet.setup(this.application!!, "https://www.baidu.com/") { obuilder, rbuilder ->
            rbuilder.addConverterFactory(ScalarsConverterFactory.create())
            val httpLoggingInterceptor = HttpLoggingInterceptor()
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY//配置输出级别
            obuilder.addInterceptor(httpLoggingInterceptor)//配置日志拦截器
        }
        val service = apiService<OkDomainTest>()

        val viewBinding = NetOkdomainActivityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        //添加一个域名
        OkDomain.setDomain("domain1", "https://juejin.cn/post/")
        //添加一个域名
        OkDomain.setDomain("domain2", "http://www.baidu.com:667/httlos/")
        //给主域名添加全局header：如果service中已经包含了该header，则全局header不会添加到请求中（通常使用这种策略）
        OkDomain.addMainHeader(TEST_HEADER, "GlobalHeaderValue", OnConflictStrategy.IGNORE)
        OkDomain.addMainHeader("customKey", "GlobalHeaderValue", OnConflictStrategy.IGNORE)
        //给域名名为‘domain1’的添加一个全局header，策略为replace，全局配置将会替换service中相同key的header（如果存在）
        OkDomain
            .addHeader("domain1", TEST_HEADER, "GlobalHeaderValue", OnConflictStrategy.REPLACE)
        //给域名名为‘domain2’的添加一个全局header，策略为add，全局配置与service中相同key的header（如果存在），均为添加到请求请求中
        OkDomain.addHeader("domain2", TEST_HEADER, "GlobalHeaderValue", OnConflictStrategy.ADD)
        //给主域名添加一个key名字为'abort_key',值为'IgnoreValue'的全局header配置，策略为abort，即如果service中已经配置相同key的header，则会导致抛出异常
        OkDomain.addMainHeader("abort_key", "GlobalHeaderValue", OnConflictStrategy.ABORT)


        viewBinding.mainRequest.setOnClickListener {
            val domain = viewBinding.mainDomainEt.text.toString()
            if (domain.isNullOrEmpty()) {
                toast("域名输入不能为空")
                return@setOnClickListener
            }
            OkDomain.setMainDomain(domain)
            request {
                service.requestBingSearch("今天星期几")
            }
        }

        viewBinding.domain1Btn.setOnClickListener {
            val domain = viewBinding.domain1.text.toString()
            if (domain.isNullOrEmpty()) {
                toast("域名输入不能为空")
                return@setOnClickListener
            }
            OkDomain.setDomain("domain1", domain)
            request {
                service.requestSeq()
            }
        }

        viewBinding.domain2Btn.setOnClickListener {
            val domain = viewBinding.domain2.text.toString()
            if (domain.isNullOrEmpty()) {
                toast("域名输入不能为空")
                return@setOnClickListener
            }
            OkDomain.setDomain("domain2", domain)
            request {
                service.requestSeq2()
            }
        }

        viewBinding.domain3Btn.setOnClickListener {
            request {
                service.requestUrl()
            }
        }

        viewBinding.headerBtn.setOnClickListener {
            request {
                service.testHeader("Querysss")
            }
        }
        viewBinding.header1Btn.setOnClickListener {
            request {
                service.testHeaderIgnore("Querysss")
            }
        }
        viewBinding.header2Btn.setOnClickListener {
            request {
                service.testHeaderReplace("测试条件")
            }
        }
        viewBinding.header3Btn.setOnClickListener {
            request {
                service.testHeaderAdd("参数")
            }
        }
        viewBinding.header4Btn.setOnClickListener {
            request {
                service.testHeaderAbort(Uri.encode("参数"))
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(message: String): ProgressDialog {
        val dialog = ProgressDialog(this)
        dialog.setMessage(message)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        return dialog
    }

    fun showAlertDialog(
        message: CharSequence,
        positiveBtnText: CharSequence
    ) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(positiveBtnText, null)
            .show()
    }

    private fun request(block: suspend () -> Any) {
        lifecycleScope.launch {
            val dialog = showLoading("正在加载")
            kotlin.runCatching {
                val result = withContext(Dispatchers.IO) {
                    block()
                }
                dialog.dismiss()
                showAlertDialog(result.toString(), "确定")
            }.onFailure {
                it.printStackTrace()
                dialog.dismiss()
                showAlertDialog(it.message.orEmpty(), "确定")
            }
        }
    }

}