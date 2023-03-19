package com.example.mystagram_2

import android.content.Intent
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.mystagram_2.databinding.ActivityLoginBinding
import com.example.mystagram_2.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class LoginActivity : AppCompatActivity() {
    val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }

    var auth : FirebaseAuth? = null
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        binding.signupBtn.setOnClickListener {

            signinAndSignup()
        }
        binding.loginBtn.setOnClickListener {

            signinEmail()
        }


    }

//    override fun onStart() {
//        super.onStart()
////        moveMainPage(auth?.currentUser)
//    }
    fun signinAndSignup(){

        var input_email = binding.inputEmail
        var input_pwd = binding.inputPwd

    // 파이어베이스 회원가입
        auth?.createUserWithEmailAndPassword(input_email.text.toString(),input_pwd.text.toString())
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    moveMainPage(task.result.user)

                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(this,task.exception?.message,Toast.LENGTH_LONG)

                }
            }




    }

    fun signinEmail(){
        // 로그인
        var input_email = binding.inputEmail
        var input_pwd = binding.inputPwd
        auth?.signInWithEmailAndPassword(input_email.text.toString(),input_pwd.text.toString())
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    moveMainPage(task.result.user)

                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(this,task.exception?.message,Toast.LENGTH_LONG)

                }
            }
    }

    fun moveMainPage(user: FirebaseUser?){

        if(user != null){
            startActivity(Intent(this,MainActivity::class.java))
//            finish()
        }
    }
}