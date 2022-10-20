package com.example.spotifyclone.other

// We could inherit from it because it is open class,we will not inherit from it here but
// it is created in a way that we could reuse it in other projects also
// The data could not be null here -> we wrap our object around this event class, we know they are not null
open class Event<out T>(private val data: T) {
    // Because we want to trigger our events a single time, initially it is false but once we trigger it
    // we will set it to true -> afterwards it wont emit this event again, instead it will emit null
    var hasBeenHandled = false
        private set

    // If the content is not handled (we call this function the first time,we get the data)
    // if we have handled it once it will just return null
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            data
        }
    }

    // Suggested by google -> if we need to get the data if it is already been handled
    fun peekContent() = data
}