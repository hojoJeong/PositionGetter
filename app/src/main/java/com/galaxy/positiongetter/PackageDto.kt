package com.galaxy.positiongetter

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PackageDto(
    var appName: String,
    var pkgName: String
) : Parcelable
