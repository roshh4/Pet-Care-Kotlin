package com.example.petcarekotlin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petcarekotlin.auth.LoginPageFragment
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)
        // Only add fragment if it's not already there (important on config changes)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginPageFragment())
                .commit()
        }
    }
}
