package com.maksimowiczm.foodyou.photo

import org.koin.dsl.module

val photoModule = module {
    single<PhotoAnalyzer> { KtorPhotoAnalyzer() }
}
