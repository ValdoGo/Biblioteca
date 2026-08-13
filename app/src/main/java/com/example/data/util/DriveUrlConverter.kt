package com.example.data.util

import java.util.regex.Pattern

object DriveUrlConverter {

    /**
     * Converte links padrão de compartilhamento do Google Drive para o link direto
     * de download/visualização: https://drive.google.com/uc?export=download&id=ID_DO_ARQUIVO
     */
    fun convertToDirectDownloadLink(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return ""

        val fileId = extractDriveFileId(trimmed)
        return if (fileId != null) {
            "https://drive.google.com/uc?export=download&confirm=t&id=$fileId"
        } else {
            trimmed
        }
    }

    /**
     * Retorna a URL otimizada para abertura no leitor Web/Google Drive.
     */
    fun getDriveViewLink(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return ""

        val fileId = extractDriveFileId(trimmed)
        return if (fileId != null) {
            "https://drive.google.com/file/d/$fileId/view"
        } else {
            trimmed
        }
    }

    /**
     * Extrai o ID único do arquivo a partir de múltiplos formatos de URL do Google Drive.
     */
    fun extractDriveFileId(url: String): String? {
        if (!url.contains("drive.google.com")) return null

        // Padrão 1: /file/d/FILE_ID/view ou /file/d/FILE_ID/edit
        val patternPath = Pattern.compile("/file/d/([a-zA-Z0-9_-]+)")
        val matcherPath = patternPath.matcher(url)
        if (matcherPath.find()) {
            return matcherPath.group(1)
        }

        // Padrão 2: id=FILE_ID em query string
        val patternParam = Pattern.compile("[?&]id=([a-zA-Z0-9_-]+)")
        val matcherParam = patternParam.matcher(url)
        if (matcherParam.find()) {
            return matcherParam.group(1)
        }

        return null
    }
}
