package com.example.mybooksapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AppViewModel: ViewModel() {
    private val _listbooks: MutableLiveData<ListBooks?>()
    val listbooks: LiveData<ListBooks> = _listbooks
}