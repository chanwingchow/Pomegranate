# Native entry points and JNA structures used by desktop OCR.
-keep class com.sun.jna.** { *; }
-keep class net.sourceforge.tess4j.** { *; }
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Kotlin serialization and service loaders need their runtime metadata.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
-keepclassmembers class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.chow.pomegranate.**$$serializer { *; }
-adaptresourcefilecontents META-INF/services/**

# Optional integrations which are not used by this JVM application.
-dontwarn okhttp3.internal.graal.**
-dontwarn okhttp3.internal.platform.BouncyCastlePlatform
-dontwarn okhttp3.internal.platform.ConscryptPlatform**
-dontwarn okhttp3.internal.platform.OpenJSSEPlatform
-dontwarn org.apache.commons.logging.impl.Log4jApiLogFactory**
-dontwarn org.apache.commons.logging.impl.Log4JLogger
-dontwarn org.apache.commons.logging.impl.AvalonLogger
-dontwarn org.apache.commons.logging.impl.LogKitLogger
-dontwarn org.apache.commons.logging.impl.ServletContextCleaner
-dontwarn org.apache.commons.logging.jakarta.ServletContextCleaner
-dontwarn org.jboss.vfs.**

# Signature-polymorphic MethodHandle calls are resolved by the JVM at runtime.
-dontwarn com.conamobile.pdfkmp.viewer.TrackpadMagnify_jvmKt
-dontwarn org.apache.pdfbox.io.IOUtils

# Unused Cloudy backend compiled against another Skia overload.
-dontwarn com.skydoves.cloudy.internal.MirageBackendProgram**
