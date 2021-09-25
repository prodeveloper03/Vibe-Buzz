package com.sounak.vibebuzz.other

open class Event<out T> (private val data:T){

    var hasBeenHandled  = false
        private  set

    fun getContentIfNotHandled():T?{

        return if(hasBeenHandled){
            return null
        }else{
            hasBeenHandled = true
            data
        }

    }

    fun peekContent () = data
}