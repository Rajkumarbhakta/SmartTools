package com.rkbapps.tooai.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity
data class QrScan(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val displayValue: String?,
    val rawVale: String?,
    val format: Int?,
    val valueType: Int?,
    val timeMillis: Long
) {
    fun getData(): String {
        return String.format(
            Locale.getDefault(),
            "Display Value: %s\nRaw Value: %s\nFormat: %s\nValue Type: %s",
            this.displayValue,
            this.rawVale,
            this.format,
            this.valueType
        )
    }
}
