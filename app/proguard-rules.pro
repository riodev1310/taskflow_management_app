# Keep Kotlin serialization classes used by Supabase decode/encode
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName *;
}

-keep @kotlinx.serialization.Serializable class *
