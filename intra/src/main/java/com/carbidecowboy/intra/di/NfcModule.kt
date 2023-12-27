package com.carbidecowboy.intra.di

import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcV
import com.carbidecowboy.intra.data.IsodepControllerImpl
import com.carbidecowboy.intra.data.NfcVControllerImpl
import com.carbidecowboy.intra.domain.NfcAdapterController
import com.carbidecowboy.intra.domain.NfcController
import com.carbidecowboy.intra.domain.OperationResult
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsodepController

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NfcVController

@Module
@InstallIn(SingletonComponent::class)
abstract class NfcModule {

    companion object {
        @Singleton
        @Provides
        fun providesNfcAdapter(@ApplicationContext context: Context): NfcAdapter {
            return NfcAdapter.getDefaultAdapter(context)
        }

        @Singleton
        @Provides
        fun providesApplicationIOCoroutineScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob() + Dispatchers.IO)
        }

        @Provides
        @Singleton
        fun provideNfcAdapterController(nfcAdapter: NfcAdapter): NfcAdapterController {
            return NfcAdapterController(nfcAdapter)
        }
    }

    @Binds
    @Singleton
    @IsodepController
    abstract fun bindIsodepController(isodepController: IsodepControllerImpl): NfcController

    @Binds
    @Singleton
    @NfcVController
    abstract fun bindNfcVController(nfcVController: NfcVControllerImpl): NfcController

    class NfcControllerFactory @Inject constructor(
        @IsodepController private val isodepControllerImpl: Provider<NfcController>,
        @NfcVController private val nfcVControllerImpl: Provider<NfcController>
    ) {
        fun getController(tag: Tag): OperationResult<NfcController> {
            return when (tag.techList.first()) {
                NfcV::class.java.name -> {
                    OperationResult.Success(nfcVControllerImpl.get())
                }
                IsoDep::class.java.name -> {
                    OperationResult.Success(isodepControllerImpl.get())
                }
                else -> {
                    OperationResult.Failure()
                }
            }
        }
    }
}