package com.blurtopian.dpolls.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.blurtopian.dpolls.data.blockchain.WalletManager
import com.blurtopian.dpolls.data.web3.Web3Service
import com.blurtopian.dpolls.domain.repository.PollsRepositoryImpl
import com.blurtopian.dpolls.presentation.navigation.PollsNavigation
import com.blurtopian.dpolls.presentation.theme.PollsDAppTheme
import dagger.hilt.android.AndroidEntryPoint
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.DefaultGasProvider
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var walletManager: WalletManager

    @Inject
    lateinit var web3Service: Web3Service

    @Inject
    lateinit var pollsRepository: PollsRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            PollsDAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PollsApp(
                        walletManager = walletManager,
                        web3Service = web3Service,
                        pollsRepository = pollsRepository
                    )
                }
            }
        }
    }

    override fun finish() {
        super.finish()
    }
}

@Composable
fun PollsApp(
    walletManager: WalletManager,
    web3Service: Web3Service,
    pollsRepository: PollsRepositoryImpl
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        PollsNavigation(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            context = context,
            walletManager = walletManager,
            web3Service = web3Service,
            pollsRepository = pollsRepository
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PollsAppPreview() {
    PollsDAppTheme {
        PollsApp(
            walletManager = WalletManager(LocalContext.current),
            web3Service = Web3Service(
                Web3j.build(HttpService("https://nero-rpc-url")),
                WalletManager(LocalContext.current),
                DefaultGasProvider()
            ),
            pollsRepository = PollsRepositoryImpl(
                Web3Service(
                    Web3j.build(HttpService("https://nero-rpc-url")),
                    WalletManager(LocalContext.current),
                    DefaultGasProvider()
                )
            )
        )
    }
}

