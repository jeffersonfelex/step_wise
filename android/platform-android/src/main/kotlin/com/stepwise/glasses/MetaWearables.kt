package com.stepwise.glasses

import android.app.Activity
import android.content.Context
import android.util.Log
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.types.RegistrationState
import com.meta.wearable.dat.core.types.WearablesError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Ciclo de vida do SDK Meta Wearables DAT, compartilhado pelos dois modos.
 *
 * O SDK precisa ser inicializado uma vez por processo antes de qualquer
 * `Wearables.createSession` (usado por [MetaDatSessionManager]), e o app precisa
 * ser registrado uma vez pelo usuário no app Meta AI para acessar os óculos.
 */
object MetaWearables {
    private const val TAG = "MetaWearables"

    /** Chamar em Application.onCreate. Idempotente; falhas ficam no log e a captura devolve null. */
    fun initialize(context: Context) {
        Wearables.initialize(context.applicationContext).onFailure { error, _ ->
            if (error != WearablesError.ALREADY_INITIALIZED) {
                Log.w(TAG, "Meta DAT não inicializado: ${error.description}")
            }
        }
    }

    /** true quando o usuário já autorizou o StepWise no Meta AI (tipos do SDK ficam neste módulo). */
    val isRegistered: Flow<Boolean>
        get() = runCatching { Wearables.registrationState.map { it == RegistrationState.REGISTERED } }
            .getOrElse { flowOf(false) } // SDK não inicializado (ex.: Meta AI ausente)

    /** Abre o Meta AI para o usuário autorizar o StepWise a usar a câmera dos óculos. false se não deu para abrir. */
    fun startRegistration(activity: Activity): Boolean =
        runCatching { Wearables.startRegistration(activity) }
            .onFailure { Log.w(TAG, "Não foi possível iniciar o registro no Meta AI.", it) }
            .isSuccess
}
