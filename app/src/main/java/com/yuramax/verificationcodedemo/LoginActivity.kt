package com.yuramax.verificationcodedemo

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import com.tencent.tauth.UiError
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONObject

class LoginActivity : AppCompatActivity() ,QQLoginUtils.QQLoginListener{

    private var qqLoginUtils: QQLoginUtils? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btnWxLogin.setOnClickListener {
            wxLogin(this)
        }

        btnQqLogin.setOnClickListener {
            qqLoginUtils = QQLoginUtils("app_id", this)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        qqLoginUtils!!.onActivityResultData(requestCode, resultCode, data)
    }

    fun wxLogin(activity: Activity){
        registToWxLogin(activity).apply {
            if (isWXAppInstalled){
                sendReq(SendAuth.Req().run {
                    //官方说明：用于保持请求和回调的状态，授权请求后原样带回给第三方。该参数可用于防止csrf攻击（跨站请求伪造攻击），
                    // 建议第三方带上该参数，可设置为简单的随机数加session进行校验
                    scope = "snsapi_userinfo"
                    state = "wechat_sdk_gg_login_state"
                    this
                })
            }else{
                toast("您的设备未安装微信客户端！")
            }
        }
    }

    private fun registToWxLogin(context: Context): IWXAPI =
        WXAPIFactory.createWXAPI(context, Configs.APP_ID,true).run {
            registerApp(Configs.APP_ID)
            this
        }

    override fun onQQLoginError(uiError: UiError?) {
        toast("登录出错！")
    }

    override fun onQQLoginSuccess(jsonObject: JSONObject?) {
        toast("登录成功！")
    }

    override fun onQQLoginCancel() {
        toast("登录取消！")
    }

    fun Activity.toast(content:String){
        Toast.makeText(this@LoginActivity,content,Toast.LENGTH_SHORT).show()
    }
}
