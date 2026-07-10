package com.deniz.pedometer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.deniz.pedometer.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tokenStore: TokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tokenStore = TokenStore(this)

        binding.registerButton.setOnClickListener { register() }
        binding.loginButton.setOnClickListener { login() }

        if (tokenStore.getToken() != null) {
            binding.statusText.text = "Giriş yapıldı"
            requestPermissionsAndStart()
        }
    }

    private fun register() {
        val username = binding.usernameInput.text.toString()
        val password = binding.passwordInput.text.toString()
        lifecycleScope.launch {
            try {
                val resp = ApiClient.service.register(RegisterRequest(username, password))
                if (resp.isSuccessful && resp.body() != null) {
                    tokenStore.saveToken(resp.body()!!.access_token)
                    binding.statusText.text = "Kayıt başarılı"
                    requestPermissionsAndStart()
                } else {
                    binding.statusText.text = "Kayıt başarısız: ${resp.code()}"
                }
            } catch (e: Exception) {
                binding.statusText.text = "Hata: ${e.message}"
            }
        }
    }

    private fun login() {
        val username = binding.usernameInput.text.toString()
        val password = binding.passwordInput.text.toString()
        lifecycleScope.launch {
            try {
                val resp = ApiClient.service.login(username, password)
                if (resp.isSuccessful && resp.body() != null) {
                    tokenStore.saveToken(resp.body()!!.access_token)
                    binding.statusText.text = "Giriş başarılı"
                    requestPermissionsAndStart()
                } else {
                    binding.statusText.text = "Giriş başarısız: ${resp.code()}"
                }
            } catch (e: Exception) {
                binding.statusText.text = "Hata: ${e.message}"
            }
        }
    }

    private fun requestPermissionsAndStart() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED
            ) needed.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            startStepService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            startStepService()
        }
    }

    private fun startStepService() {
        val intent = Intent(this, StepCounterService::class.java)
        ContextCompat.startForegroundService(this, intent)
        binding.stepCountText.text = "Adım sayar arka planda çalışıyor"
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 42
    }
}
