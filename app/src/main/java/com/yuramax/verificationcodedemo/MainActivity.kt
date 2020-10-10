package com.yuramax.verificationcodedemo

import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val piv: PasswordInputView = findViewById(R.id.piv)
        piv.apply {
            setPivCursorVisible(true)
            setInputListener(object : PasswordInputView.InputListener {
                override fun onInputCompleted(text: String?) {
                    Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
                    Handler().postDelayed({ piv.setText("") }, 200)
                }
            })
        }
    }
}
