package io.dee.portal.core.view.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import io.dee.portal.R
import io.dee.portal.core.view.MainViewModel
import io.dee.portal.databinding.ActivityMainBinding
import io.dee.portal.utils.ConnectivityUtil
import io.dee.portal.utils.NetworkStatus
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController
    private lateinit var connectivityUtil: ConnectivityUtil
    private var backPressedOnce: Boolean = false

    private val onBackPressedCallback = object : OnBackPressedCallback(enabled = true) {
        override fun handleOnBackPressed() {
            if (backPressedOnce) {
                finish()
                return
            }
            backPressedOnce = true
            Toast.makeText(
                this@MainActivity, getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT
            ).show()
            Handler(Looper.getMainLooper()).postDelayed({
                backPressedOnce = false
            }, 2000)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        bindBackPressDispatcher()
        connectivityUtil = ConnectivityUtil(this)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                connectivityUtil.networkStatus.collect {
                    viewModel.setConnectivityStatus(it)
                        if (it == NetworkStatus.Disconnected) {
                        binding.llNoInternet.visibility = View.VISIBLE
                    } else {
                        binding.llNoInternet.visibility = View.GONE
                    }
                }
            }
        }
        binding.llNoInternet.setOnClickListener {

            binding.llNoInternet.visibility = View.GONE
        }
    }

    private fun bindBackPressDispatcher() {
        onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityUtil.unregister()
    }
}
