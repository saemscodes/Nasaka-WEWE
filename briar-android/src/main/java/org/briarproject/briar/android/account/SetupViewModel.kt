package org.briarproject.briar.android.account

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.briarproject.android.dontkillmelib.DozeHelper
import org.briarproject.bramble.api.account.AccountManager
import org.briarproject.bramble.api.crypto.PasswordStrengthEstimator
import org.briarproject.bramble.api.lifecycle.IoExecutor
import org.briarproject.briar.android.viewmodel.LiveEvent
import org.briarproject.briar.android.viewmodel.MutableLiveEvent
import java.util.concurrent.Executor
import java.util.logging.Logger
import javax.inject.Inject

class SetupViewModel @Inject constructor(
    app: Application,
    private val accountManager: AccountManager,
    @IoExecutor private val ioExecutor: Executor,
    private val strengthEstimator: PasswordStrengthEstimator,
    private val dozeHelper: DozeHelper
) : AndroidViewModel(app) {

    enum class State { AUTHOR_NAME, SET_PASSWORD, DOZE, CREATED, FAILED }

    private val LOG = Logger.getLogger(SetupViewModel::class.java.name)

    private var authorName: String? = null
    private var password: String? = null
    
    private val _state = MutableLiveEvent<State>()
    val state: LiveEvent<State> = _state

    private val _isCreatingAccount = MutableLiveData(false)
    val isCreatingAccount: LiveData<Boolean> = _isCreatingAccount

    init {
        ioExecutor.execute {
            if (accountManager.accountExists()) {
                // Should not happen during setup
            } else {
                _state.postEvent(State.AUTHOR_NAME)
            }
        }
    }

    fun createAccount(name: String, pass: String, role: Int) {
        authorName = name
        password = pass
        _isCreatingAccount.value = true
        ioExecutor.execute {
            if (accountManager.createAccount(name, pass, role)) {
                LOG.info("Created account")
                _state.postEvent(State.CREATED)
            } else {
                LOG.warning("Failed to create account")
                _state.postEvent(State.FAILED)
            }
        }
    }
}
