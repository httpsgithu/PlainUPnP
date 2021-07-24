package com.m3sv.plainupnp.upnp

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.m3sv.plainupnp.ContentModel
import com.m3sv.plainupnp.ContentRepository
import com.m3sv.plainupnp.common.preferences.PreferencesRepository
import com.m3sv.plainupnp.logging.Log
import com.m3sv.plainupnp.upnp.mediacontainers.*
import com.m3sv.plainupnp.upnp.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

typealias ContentCache = MutableMap<Long, BaseContainer>

sealed class ContentUpdateState {
    object Loading : ContentUpdateState()
    data class Ready(val data: ContentCache) : ContentUpdateState()
}

@Singleton
class UpnpContentRepositoryImpl @Inject constructor(
    private val application: Application,
    private val preferencesRepository: PreferencesRepository,
    private val log: Log
) : ContentRepository {

    val containerCache: MutableMap<Long, BaseContainer> = mutableMapOf()
    private val allCache: MutableMap<Long, ContentModel> = mutableMapOf()
    override val contentCache: Map<Long, ContentModel> = allCache

    private val scope = CoroutineScope(Dispatchers.IO)
    private val appName by lazy { application.getString(R.string.app_name) }
    private val baseUrl by lazy { "${getLocalIpAddress(application, log).hostAddress}:$PORT" }
    private val refreshInternal = MutableSharedFlow<Unit>()
    private val _refreshState: MutableStateFlow<ContentUpdateState> =
        MutableStateFlow(ContentUpdateState.Ready(containerCache))

    val refreshState: Flow<ContentUpdateState> = _refreshState

    init {
        scope.launch {
            refreshInternal.onEach {
                _refreshState.value = ContentUpdateState.Loading
                refreshInternal()
                _refreshState.value = ContentUpdateState.Ready(containerCache)
            }.collect()
        }

        scope.launch {
            preferencesRepository
                .updateFlow
                .debounce(2000)
                .collect { refreshContent() }
        }
    }

    override fun refreshContent() {
        scope.launch { refreshInternal.emit(Unit) }
    }

    private val init by lazy {
        runBlocking {
            refreshInternal()
        }
    }

    override fun init() {
        init
    }

    private suspend fun refreshInternal() = coroutineScope {
        containerCache.clear()

        val rootContainer = createRootContainer().also { container -> container.addToRegistry() }

        preferencesRepository.preferences.value.let { preferences ->
            if (preferences.enableImages) {
                launch {
                    getRootImagesContainer()
                        .also(rootContainer::addContainer)
                        .addToRegistry()
                }
            }

            if (preferences.enableAudio) {
                launch {
                    getRootAudioContainer(rootContainer).addToRegistry()
                }
            }

            if (preferences.enableVideos) {
                launch {
                    getRootVideoContainer(rootContainer).addToRegistry()
                }
            }
        }

        launch { getUserSelectedContainer(rootContainer) }
    }

    private fun createRootContainer() = Container(
        ROOT_ID.toString(),
        ROOT_ID.toString(),
        appName,
        appName
    )

    fun getAudioContainerForAlbum(
        albumId: String,
        parentId: String,
    ): BaseContainer = AllAudioContainer(
        id = albumId,
        parentID = parentId,
        title = "",
        creator = appName,
        baseUrl = baseUrl,
        contentResolver = application.contentResolver,
        albumId = albumId,
        artist = null
    )

    fun getAlbumContainerForArtist(
        artistId: String,
        parentId: String,
    ): AlbumContainer = AlbumContainer(
        id = artistId,
        parentID = parentId,
        title = "",
        creator = appName,
        log = log,
        baseUrl = baseUrl,
        contentResolver = application.contentResolver,
        artistId = artistId
    )

    private fun BaseContainer.addToRegistry() {
        containerCache[rawId.toLong()] = this
    }

    private fun getRootImagesContainer(): BaseContainer {
        val rootContainer = Container(
            IMAGE_ID.toString(),
            ROOT_ID.toString(),
            application.getString(R.string.images),
            appName
        )

        AllImagesContainer(
            id = ALL_IMAGE.toString(),
            parentID = IMAGE_ID.toString(),
            title = application.getString(R.string.all),
            creator = appName,
            baseUrl = baseUrl,
            contentResolver = application.contentResolver
        ).also { container ->
            rootContainer.addContainer(container)
            container.addToRegistry()
        }

        Container(
            IMAGE_BY_FOLDER.toString(),
            rootContainer.id,
            application.getString(R.string.by_folder),
            appName
        ).also { container ->
            rootContainer.addContainer(container)
            container.addToRegistry()

            val column = ImageDirectoryContainer.IMAGE_DATA_PATH
            val externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            generateContainerStructure(
                column,
                container,
                externalContentUri
            ) { id, parentID, title, creator, baseUrl, contentDirectory, contentResolver ->
                ImageDirectoryContainer(
                    id = id,
                    parentID = parentID,
                    title = title,
                    creator = creator,
                    baseUrl = baseUrl,
                    directory = contentDirectory,
                    contentResolver = contentResolver
                )
            }
        }

        return rootContainer
    }

    private fun getRootAudioContainer(rootContainer: BaseContainer): BaseContainer =
        Container(
            AUDIO_ID.toString(),
            ROOT_ID.toString(),
            application.getString(R.string.audio),
            appName
        ).apply {
            rootContainer.addContainer(this)

            AllAudioContainer(
                ALL_AUDIO.toString(),
                AUDIO_ID.toString(),
                application.getString(R.string.all),
                appName,
                baseUrl = baseUrl,
                contentResolver = application.contentResolver,
                albumId = null,
                artist = null
            ).also { container ->
                addContainer(container)
                container.addToRegistry()
            }

            ArtistContainer(
                ALL_ARTISTS.toString(),
                AUDIO_ID.toString(),
                application.getString(R.string.artist),
                appName,
                log,
                baseUrl,
                application.contentResolver
            ).also { container ->
                addContainer(container)
                container.addToRegistry()
            }

            AlbumContainer(
                id = ALL_ALBUMS.toString(),
                parentID = AUDIO_ID.toString(),
                title = application.getString(R.string.album),
                creator = appName,
                log = log,
                baseUrl = baseUrl,
                contentResolver = application.contentResolver,
                artistId = null
            ).also { container ->
                addContainer(container)
                container.addToRegistry()
            }

            Container(
                AUDIO_BY_FOLDER.toString(),
                id,
                application.getString(R.string.by_folder),
                appName
            ).also { container ->
                addContainer(container)
                container.addToRegistry()

                val column = AudioDirectoryContainer.AUDIO_DATA_PATH
                val externalContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

                generateContainerStructure(
                    column,
                    container,
                    externalContentUri
                ) { id, parentID, title, creator, baseUrl, contentDirectory, contentResolver ->
                    AudioDirectoryContainer(
                        id = id,
                        parentID = parentID,
                        title = title,
                        creator = creator,
                        baseUrl = baseUrl,
                        directory = contentDirectory,
                        contentResolver = contentResolver
                    )
                }
            }
        }

    private fun getRootVideoContainer(rootContainer: BaseContainer): BaseContainer =
        Container(
            VIDEO_ID.toString(),
            ROOT_ID.toString(),
            application.getString(R.string.videos),
            appName
        ).apply {
            rootContainer.addContainer(this)

            AllVideoContainer(
                ALL_VIDEO.toString(),
                VIDEO_ID.toString(),
                application.getString(R.string.all),
                appName,
                baseUrl,
                contentResolver = application.contentResolver
            ).also { container ->
                addContainer(container)
                container.addToRegistry()
            }

            Container(
                VIDEO_BY_FOLDER.toString(),
                id,
                application.getString(R.string.by_folder),
                appName
            ).also { container ->
                addContainer(container)
                container.addToRegistry()

                val column = VideoDirectoryContainer.VIDEO_DATA_PATH
                val externalContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

                generateContainerStructure(
                    column,
                    container,
                    externalContentUri
                ) { id, parentID, title, creator, baseUrl, contentDirectory, contentResolver ->
                    VideoDirectoryContainer(
                        id = id,
                        parentID = parentID,
                        title = title,
                        creator = creator,
                        baseUrl = baseUrl,
                        directory = contentDirectory,
                        contentResolver = contentResolver
                    )
                }
            }
        }

    private suspend fun getUserSelectedContainer(rootContainer: Container) {
        coroutineScope {
            application
                .contentResolver
                .persistedUriPermissions
                .forEach { urlPermission ->
                    launch {
                        val displayName = DocumentFile.fromTreeUri(application, urlPermission.uri)?.name
                        val documentId = DocumentsContract.getTreeDocumentId(urlPermission.uri)
                        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(urlPermission.uri, documentId)

                        if (uri != null && displayName != null) {
                            queryUri(uri, rootContainer, displayName)
                        }
                    }
                }
        }
    }

    private suspend fun queryUri(
        uri: Uri,
        parentContainer: Container,
        newContainerName: String
    ) {
        coroutineScope {
            val newContainer = createContainer(randomId, parentId = parentContainer.rawId, newContainerName)
            parentContainer.addContainer(newContainer)
            newContainer.addToRegistry()

            val resolver = application.contentResolver
            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getDocumentId(uri))

            resolver.query(
                childrenUri,
                mediaColumns,
                null,
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val newDocumentUri = DocumentsContract.buildDocumentUriUsingTree(uri, cursor.getString(0))
                    val mimeType = cursor.getString(1)
                    val displayName = cursor.getString(2)
                    val size = cursor.getLong(3)
                    val artist = cursor.getString(4)
                    val albumArtist = cursor.getString(5)
                    val album = cursor.getString(6)

                    when {
                        mimeType == DocumentsContract.Document.MIME_TYPE_DIR -> launch {
                            queryUri(
                                newDocumentUri,
                                newContainer,
                                displayName
                            )
                        }
                        mimeType != DocumentsContract.Document.MIME_TYPE_DIR && mimeType.isNotEmpty() -> {
                            addFile(
                                newContainer,
                                newDocumentUri,
                                displayName,
                                mimeType,
                                size,
                                null,
                                album,
                                albumArtist
                            )
                        }
                    }
                }
            }
        }
    }

    private fun addFile(
        parentContainer: Container,
        uri: Uri,
        displayName: String,
        mime: String,
        size: Long,
        duration: Long?,
        album: String?,
        creator: String?
    ) {
        val id = randomId
        val itemId = "${TREE_PREFIX}${id}"
        val item = when {
            mime.startsWith("image") -> parentContainer.addImageItem(
                baseUrl = baseUrl,
                id = itemId,
                name = displayName,
                mime = mime,
                width = 0,
                height = 0,
                size = size
            )

            mime.startsWith("audio") -> parentContainer.addAudioItem(
                baseUrl = baseUrl,
                id = itemId,
                name = displayName,
                mime = mime,
                width = 0,
                height = 0,
                size = size,
                duration = duration ?: 0L,
                album = album ?: "",
                creator = creator ?: ""
            )

            mime.startsWith("video") -> parentContainer.addVideoItem(
                baseUrl = baseUrl,
                id = itemId,
                name = displayName,
                mime = mime,
                width = 0,
                height = 0,
                size = size,
                duration = duration ?: 0L
            )

            else -> null
        }

        if (item != null) {
            allCache[id] = ContentModel(uri, mime, displayName, item)
        }
    }

    private fun createContainer(
        id: Long,
        parentId: String,
        name: String?,
    ) = Container(id.toString(), parentId, "${USER_DEFINED_PREFIX}$name", null)

    private fun generateContainerStructure(
        column: String,
        rootContainer: BaseContainer,
        externalContentUri: Uri,
        childContainerBuilder: (
            id: String,
            parentID: String?,
            title: String,
            creator: String,
            baseUrl: String,
            contentDirectory: ContentDirectory,
            contentResolver: ContentResolver,
        ) -> BaseContainer,
    ) {
        val folders: MutableMap<String, Map<String, Any>> = mutableMapOf()
        buildSet<String> {
            application.contentResolver.query(
                externalContentUri,
                arrayOf(column),
                null,
                null,
                null
            )?.use { cursor ->
                val pathColumn = cursor.getColumnIndexOrThrow(column)

                while (cursor.moveToNext()) {
                    cursor
                        .getString(pathColumn)
                        .let { path ->
                            when {
                                path.startsWith("/") -> path.drop(1)
                                path.endsWith("/") -> path.dropLast(1)
                                else -> path
                            }
                        }.also(::add)
                }
            }
        }.map { it.split("/") }.forEach {
            lateinit var map: MutableMap<String, Map<String, Any>>

            it.forEachIndexed { index, s ->
                if (index == 0) {
                    if (folders[s] == null)
                        folders[s] = mutableMapOf<String, Map<String, Any>>()

                    map = folders[s] as MutableMap<String, Map<String, Any>>
                } else {
                    if (map[s] == null)
                        map[s] = mutableMapOf<String, Map<String, Any>>()

                    map = map[s] as MutableMap<String, Map<String, Any>>
                }
            }
        }


        fun populateFromMap(rootContainer: BaseContainer, map: Map<String, Map<String, Any>>) {
            map.forEach { kv ->
                val childContainer = childContainerBuilder(
                    randomId.toString(),
                    rootContainer.rawId,
                    kv.key,
                    appName,
                    baseUrl,
                    ContentDirectory(kv.key),
                    application.contentResolver
                ).apply { addToRegistry() }

                rootContainer.addContainer(childContainer)

                populateFromMap(childContainer, kv.value as Map<String, Map<String, Any>>)
            }
        }

        populateFromMap(rootContainer, folders)
    }

    companion object {
        const val USER_DEFINED_PREFIX = "USER_DEFINED_"
        const val SEPARATOR = '$'

        // Type
        const val ROOT_ID: Long = 0
        const val IMAGE_ID: Long = 1
        const val AUDIO_ID: Long = 2
        const val VIDEO_ID: Long = 3

        // Type subfolder
        const val ALL_IMAGE: Long = 10
        const val ALL_VIDEO: Long = 20
        const val ALL_AUDIO: Long = 30

        const val IMAGE_BY_FOLDER: Long = 100
        const val VIDEO_BY_FOLDER: Long = 200
        const val AUDIO_BY_FOLDER: Long = 300

        const val ALL_ARTISTS: Long = 301
        const val ALL_ALBUMS: Long = 302

        // Prefix item
        const val VIDEO_PREFIX = "v-"
        const val AUDIO_PREFIX = "a-"
        const val IMAGE_PREFIX = "i-"
        const val TREE_PREFIX = "t-"

        private val random = SecureRandom()
        private val randomId
            get() = abs(random.nextLong())

        private val mediaColumns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            MediaStore.MediaColumns.ARTIST,
            MediaStore.MediaColumns.ALBUM_ARTIST,
            MediaStore.MediaColumns.ALBUM
        )
    }
}
