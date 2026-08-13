package com.example.data.config

import com.example.BuildConfig

/**
 * Estrutura de Configuração e Credenciais do Firebase.
 * Permite preencher as chaves do Firebase Console via BuildConfig (.env)
 * ou visualizar/editar dinamicamente na aplicação.
 */
data class FirebaseConfig(
    val apiKey: String = BuildConfig.FIREBASE_API_KEY.ifEmpty { "AIzaSyBC6vJXQ-HoB2wK4bZ1Gbh1uE0hg7afQSU" },
    val authDomain: String = BuildConfig.FIREBASE_AUTH_DOMAIN.ifEmpty { "biblioteca-c8737.firebaseapp.com" },
    val projectId: String = BuildConfig.FIREBASE_PROJECT_ID.ifEmpty { "biblioteca-c8737" },
    val storageBucket: String = BuildConfig.FIREBASE_STORAGE_BUCKET.ifEmpty { "biblioteca-c8737.firebasestorage.app" },
    val messagingSenderId: String = BuildConfig.FIREBASE_MESSAGING_SENDER_ID.ifEmpty { "774805728240" },
    val appId: String = BuildConfig.FIREBASE_APP_ID.ifEmpty { "1:774805728240:web:d06b2614ef800b3c188b35" }
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && 
                !apiKey.contains("YOUR_") && 
                projectId.isNotBlank() && 
                !projectId.contains("your-")

    fun getFormattedSummary(): String {
        return """
            Project ID: $projectId
            Auth Domain: $authDomain
            Storage Bucket: $storageBucket
            App ID: $appId
            Status: ${if (isConfigured) "Conectado ao Firebase Console" else "Modo de Teste Local Ativo"}
        """.trimIndent()
    }
}
