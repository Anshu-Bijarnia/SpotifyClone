package com.example.spotifyclone.other

// out T means we can pass the parent classes of this T also
data class Resource<out T> (val status: Status, val data : T?,val message : String?){
    companion object {
        // In success case a message is not needed because we know the resource was successful
        fun <T> success (data : T?) = Resource(Status.SUCCESS,data,null)
        // In case of an error, a error message is compulsory for the user. There can be some data to show
        // even in the case of error
        fun <T> error ( message : String,data : T?) = Resource(Status.ERROR,data,message)
        // Could have data already while we are loading, for example if caching mechanism is implemented -> we can have
        // data from the cache while data is being loaded.
        fun <T> loading (data : T?) = Resource (Status.LOADING,data,null)
    }
}
// The describes the status a resource can have
enum class Status {
    SUCCESS,
    ERROR,
    LOADING // useful when our media is loading to show progress bar in our fragment/activity
}