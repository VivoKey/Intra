package com.hoker.intra.di

import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import android.nfc.tech.NfcV
import com.hoker.intra.data.IsodepControllerImpl
import com.hoker.intra.data.NfcAControllerImpl
import com.hoker.intra.data.NfcVControllerImpl
import com.hoker.intra.domain.NfcAdapterController
import com.hoker.intra.domain.NfcController
import com.hoker.intra.domain.OperationResult
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

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NfcAController

@Module
@InstallIn(SingletonComponent::class)
abstract class NfcModule {

    companion object {
        @Singleton
        @Provides
        fun providesNfcAdapter(@ApplicationContext context: Context): NfcAdapter? {
            return NfcAdapter.getDefaultAdapter(context)
        }

        @Singleton
        @Provides
        fun providesApplicationIOCoroutineScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob() + Dispatchers.IO)
        }

        @Provides
        @Singleton
        fun provideNfcAdapterController(
            nfcAdapter: NfcAdapter?,
            nfcControllerFactory: NfcControllerFactory
        ): NfcAdapterController {
            return NfcAdapterController(nfcAdapter, nfcControllerFactory)
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

    @Binds
    @Singleton
    @NfcAController
    abstract fun bindNfcAController(nfcAController: NfcAControllerImpl): NfcController

    class NfcControllerFactory @Inject constructor(
        @IsodepController private val isodepControllerImpl: Provider<NfcController>,
        @NfcVController private val nfcVControllerImpl: Provider<NfcController>,
        @NfcAController private val nfcAControllerImpl: Provider<NfcController>
    ) {
        fun getController(tag: Tag): OperationResult<NfcController> {
            return when (tag.techList.first()) {
                NfcV::class.java.name -> {
                    OperationResult.Success(nfcVControllerImpl.get())
                }
                IsoDep::class.java.name -> {
                    OperationResult.Success(isodepControllerImpl.get())
                }
                NfcA::class.java.name -> {
                    OperationResult.Success(nfcAControllerImpl.get())
                }
                else -> {
                    OperationResult.Failure()
                }
            }
        }
    }
}