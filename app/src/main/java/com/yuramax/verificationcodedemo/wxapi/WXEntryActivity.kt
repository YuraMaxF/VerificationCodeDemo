package com.yuramax.verificationcodedemo.wxapi

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.StringBuilder
import okhttp3.FormBody
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.yuramax.verificationcodedemo.Configs
import com.yuramax.verificationcodedemo.MainActivity
import com.yuramax.verificationcodedemo.SPUtils
import com.yuramax.verificationcodedemo.WX


/**
 * author : weijun
 * e-mail : 1301892339@qq.com
 * time   : 2020/01/17
 * desc   : 微信登录回调
 * version: 1.0
 */
class WXEntryActivity : AppCompatActivity(),
        IWXAPIEventHandler {

    private lateinit var iwxapi: IWXAPI
    private val mProgressDialog by lazy {
        ProgressDialog(this).run {
            setProgressStyle(ProgressDialog.STYLE_SPINNER) //转盘
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            setTitle("提示")
            setMessage("登录中，请稍后")
            this
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        "进入 WXEntryActivity".logE()
        supportActionBar?.hide()
        window?.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        //接收到分享以及登录的intent传递handleIntent方法，处理结果
        iwxapi = WXAPIFactory.createWXAPI(this, Configs.APP_ID, true).run {
            handleIntent(intent!!, this@WXEntryActivity)
            this
        }
    }

    override fun onResp(p0: BaseResp?) {
        when (p0!!.errCode) {
            BaseResp.ErrCode.ERR_OK -> { //获取 access_token
                getAccessToken((p0 as SendAuth.Resp).code)
            }
            BaseResp.ErrCode.ERR_AUTH_DENIED -> finish() //用户拒绝授权
            BaseResp.ErrCode.ERR_USER_CANCEL -> finish() //用户取消
        }
    }

    override fun onReq(p0: BaseReq?) {

    }

    private fun getAccessToken(code: String) {
        mProgressDialog.show()
        //获取授权
        val loginUrl = StringBuilder().run {
            append("https://api.weixin.qq.com/sns/oauth2/access_token")
            append("?appid=")
            append(Configs.APP_ID)
            append("&secret=")
            append(Configs.APP_SERECET)
            append("&code=")
            append(code)
            append("&grant_type=authorization_code")
        }
        loginUrl.toString().logE("loginUrl")

        OkHttpClient().newCall(
                Request.Builder()
                        .url(loginUrl.toString())
                        .get() //默认就是GET请求，可以不写
                        .build()
        ).apply {
            enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    response.body()?.string()?.let {
                        "onResponse: $it".logE("getAccessToken")
                        var access = ""
                        var openId = ""
                        try {
                            JSONObject(it).apply {
                                access = getString("access_token")
                                openId = getString("openid")
                                SPUtils.put(this@WXEntryActivity, "open_id", openId)
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                        getUserInfo(access, openId)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    mProgressDialog.dismiss()
                }

            })
        }
    }

    private fun getUserInfo(access: String, openid: String) {
        val getUserInfoUrl =
                "https://api.weixin.qq.com/sns/userinfo?access_token=$access&openid=$openid"
        OkHttpClient().newCall(
                Request.Builder()
                        .url(getUserInfoUrl)
                        .get()//默认就是GET请求，可以不写
                        .build()
        ).apply {
            enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    mProgressDialog.dismiss()
                }

                override fun onResponse(call: Call, response: Response) {
                    response.body()?.string()?.let {
                        "onResponse: $it".logE("getUserInfo")
                        SPUtils.put(this@WXEntryActivity, "access_token", it)
                    }
                    //微信登录成功回调
                    requestForToken()
                }
            })
        }
    }

    val BASE_URL = ""

    private fun requestForToken() {
        val url = BASE_URL + "api/wLogin"
        OkHttpClient().newCall(
                Request.Builder()
                        .url(url)
                        .post(FormBody.create(
                                MediaType.parse("application/json; charset=utf-8"),
                                Gson().toJson(WX().run {
                                    accessToken = SPUtils.get(this@WXEntryActivity, "access_token", "") as String
                                    openId = SPUtils.get(this@WXEntryActivity, "open_id", "") as String
                                    this
                                }
                                )
                        ))
                        .build()
        ).apply {
            enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    mProgressDialog.dismiss()
                }

                override fun onResponse(call: Call, response: Response) {
                    response.body()?.string()?.let {
                        it.logE()
                        val jsonObject = JSONObject(it)
                        val data = jsonObject.getJSONObject("data")
                        SPUtils.saveToken(applicationContext, data.getString("token"))
                    }
                    startActivity(Intent(this@WXEntryActivity, MainActivity::class.java).run {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        this
                    })
                    finish()
                    mProgressDialog.dismiss()
                }
            })
        }
    }

    fun String.logE(tag: String = ""){
        Log.e(tag,this)
    }
}