package com.clubnexus.android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.clubnexus.android.data.auth.SessionStore

class SessionViewModelFactory(private val store: SessionStore) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SessionViewModel(store) as T
}
