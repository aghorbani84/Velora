package com.example.di

import androidx.room.Room
import com.example.data.local.FazoraDatabase
import com.example.data.playback.Media3PlayerEngine
import com.example.data.repository.FazoraLibraryRepository
import com.example.data.scanner.MediaLibraryScanner
import com.example.data.settings.FazoraSettingsRepository
import com.example.data.firebase.FazoraFirebaseRepository
import com.example.domain.engine.MediaOrchestrator
import com.example.domain.engine.PlayerEngine
import com.example.orchestrator.IMediaOrchestrator
import com.example.ui.viewmodel.FazoraViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val fazoraAppModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            FazoraDatabase::class.java,
            "fazora_library.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    single { get<FazoraDatabase>().trackDao() }
    single { get<FazoraDatabase>().playlistDao() }
    single { get<FazoraDatabase>().eqPresetDao() }
    single { get<FazoraDatabase>().playbackHistoryDao() }

    single { MediaLibraryScanner(androidContext()) }
    single { FazoraSettingsRepository(androidContext()) }

    single<PlayerEngine> { Media3PlayerEngine(androidContext()) }

    single {
        FazoraLibraryRepository(
            context = androidContext(),
            trackDao = get(),
            playlistDao = get(),
            eqPresetDao = get(),
            scanner = get(),
            playbackHistoryDao = get()
        )
    }

    single {
        MediaOrchestrator(
            repository = get(),
            settingsRepository = get(),
            playerEngine = get()
        )
    }

    single<IMediaOrchestrator> { get<MediaOrchestrator>() }

    single { FazoraFirebaseRepository(androidContext()) }

    viewModel {
        FazoraViewModel(
            repository = get(),
            settingsRepository = get(),
            playerEngine = get(),
            firebaseRepository = get(),
            mediaOrchestrator = get()
        )
    }
}
