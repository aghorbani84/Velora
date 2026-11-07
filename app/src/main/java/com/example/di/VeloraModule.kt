package com.example.di

import androidx.room.Room
import com.example.data.firebase.VeloraFirebaseRepository
import com.example.data.local.VeloraDatabase
import com.example.data.playback.Media3PlayerEngine
import com.example.data.repository.VeloraLibraryRepository
import com.example.data.scanner.MediaLibraryScanner
import com.example.data.settings.VeloraSettingsRepository
import com.example.domain.engine.MediaOrchestrator
import com.example.domain.engine.PlayerEngine
import com.example.orchestrator.IMediaOrchestrator
import com.example.ui.viewmodel.VeloraViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val veloraAppModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            VeloraDatabase::class.java,
            "velora_library.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    single { get<VeloraDatabase>().trackDao() }
    single { get<VeloraDatabase>().playlistDao() }
    single { get<VeloraDatabase>().eqPresetDao() }
    single { get<VeloraDatabase>().playbackHistoryDao() }

    single { MediaLibraryScanner(androidContext()) }
    single { VeloraSettingsRepository(androidContext()) }

    single<PlayerEngine> { Media3PlayerEngine(androidContext()) }

    single {
        VeloraLibraryRepository(
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

    single { VeloraFirebaseRepository(androidContext()) }

    viewModel {
        VeloraViewModel(
            repository = get(),
            settingsRepository = get(),
            playerEngine = get(),
            firebaseRepository = get(),
            mediaOrchestrator = get()
        )
    }
}

