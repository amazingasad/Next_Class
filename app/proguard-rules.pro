-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, RuntimeVisibleAnnotations, EnclosingMethod

# Kotlinx serialization
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.example.nextclass.**$$serializer { *; }
-keepclassmembers class com.example.nextclass.** { *** Companion; }
-keepclasseswithmembers class com.example.nextclass.** { kotlinx.serialization.KSerializer serializer(...); }

# Retrofit service and suspend-function continuation
-keep interface com.example.nextclass.data.remote.GeminiApiService { *; }
-keep class kotlin.coroutines.Continuation
-keep class com.example.nextclass.data.remote.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Glance widget and receivers
-keep class com.example.nextclass.widget.** { *; }
-keep class com.example.nextclass.worker.** { *; }

# Warnings from transitive dependencies
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.crypto.tink.**
-dontwarn okhttp3.internal.platform.**
