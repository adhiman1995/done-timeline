# kotlinx.serialization ships its own consumer rules; keep our DTOs' generated serializers.
-keepclassmembers @kotlinx.serialization.Serializable class com.donetimeline.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
