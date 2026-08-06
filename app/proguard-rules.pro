# kotlinx.serialization keeps its generated serializers via companion objects.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.orphybel.bacine.data.remote.dto.** {
    *** Companion;
}
-keepclasseswithmembers class com.orphybel.bacine.data.remote.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp ships optional Conscrypt/BouncyCastle hooks that are absent at runtime.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**


# The YouTube player drives the IFrame API from a WebView.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
