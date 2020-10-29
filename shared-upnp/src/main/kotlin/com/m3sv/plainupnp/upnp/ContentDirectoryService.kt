package com.m3sv.plainupnp.upnp

import android.content.Context
import android.content.SharedPreferences
import android.provider.MediaStore
import com.m3sv.plainupnp.upnp.discovery.file.FileHierarchyBuilder
import com.m3sv.plainupnp.upnp.mediacontainers.*
import com.m3sv.plainupnp.upnp.util.CONTENT_DIRECTORY_AUDIO
import com.m3sv.plainupnp.upnp.util.CONTENT_DIRECTORY_IMAGE
import com.m3sv.plainupnp.upnp.util.CONTENT_DIRECTORY_VIDEO
import kotlinx.coroutines.*
import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException
import org.fourthline.cling.support.contentdirectory.DIDLParser
import org.fourthline.cling.support.model.BrowseFlag
import org.fourthline.cling.support.model.BrowseResult
import org.fourthline.cling.support.model.DIDLContent
import org.fourthline.cling.support.model.SortCriterion
import timber.log.Timber
import kotlin.LazyThreadSafetyMode.NONE

class ContentDirectoryService : AbstractContentDirectoryService() {

    lateinit var context: Context

    lateinit var baseURL: String

    lateinit var sharedPref: SharedPreferences

    private val appName by lazy(NONE) { context.getString(R.string.app_name) }

    private val containerRegistry: MutableMap<Int, BaseContainer> = mutableMapOf()

    override fun browse(
        objectID: String,
        browseFlag: BrowseFlag,
        filter: String,
        firstResult: Long,
        maxResults: Long,
        orderby: Array<SortCriterion>
    ): BrowseResult = runBlocking {
        try {
            var root = -1
            val subtype = mutableListOf<Int>()

            objectID
                .split(SEPARATOR)
                .map(Integer::parseInt)
                .forEach {
                    if (root == -1) {
                        root = it

                        if (root != ROOT_ID
                            && root != VIDEO_ID
                            && root != AUDIO_ID
                            && root != IMAGE_ID
                            && root !in containerRegistry.keys
                        ) {
                            throw ContentDirectoryException(
                                ContentDirectoryErrorCode.NO_SUCH_OBJECT,
                                "Invalid type!"
                            )
                        }
                    } else {
                        subtype.add(it)
                    }
                }

            Timber.d("Browsing type $root")
            if (containerRegistry[ROOT_ID] == null) {
                containerRegistry[ROOT_ID] = Container(
                    ROOT_ID.toString(),
                    ROOT_ID.toString(),
                    appName,
                    appName
                )
            }

            val rootContainer: BaseContainer = requireNotNull(containerRegistry[ROOT_ID])

            val jobs = mutableListOf<Job>()

            if (isImagesEnabled && containerRegistry[IMAGE_ID] == null) {
                jobs += launch(Dispatchers.IO) {
                    containerRegistry[IMAGE_ID] = getRootImagesContainer(rootContainer)
                }
            }

            if (isAudioEnabled && containerRegistry[AUDIO_ID] == null) {
                jobs += launch(Dispatchers.IO) {
                    containerRegistry[AUDIO_ID] = getRootAudioContainer(rootContainer)
                }
            }

            if (isVideoEnabled && containerRegistry[VIDEO_ID] == null) {
                jobs += launch(Dispatchers.IO) {
                    containerRegistry[VIDEO_ID] = getRootVideoContainer(rootContainer)
                }
            }

            jobs.joinAll()

            val container: BaseContainer = if (subtype.isEmpty()) {
                containerRegistry[root] ?: throw noSuchObject
            } else {
                when (root) {
                    VIDEO_ID -> containerRegistry[subtype[0]] ?: throw noSuchObject
                    AUDIO_ID -> when {
                        subtype.size == 1 -> containerRegistry[subtype[0]] ?: throw noSuchObject
                        subtype.size == 2 && subtype[0] == ALL_ARTISTS -> {
                            val artistId = subtype[1].toString()
                            val parentId = "$AUDIO_ID$SEPARATOR${subtype[0]}"
                            Timber.d("Listing album of artist $artistId")

                            AlbumContainer(
                                artistId,
                                parentId,
                                "",
                                appName,
                                baseURL,
                                context.contentResolver,
                                artistId = artistId
                            )
                        }
                        subtype.size == 2 && subtype[0] == ALL_ALBUMS -> {
                            val albumId = subtype[1].toString()
                            val parentId = "$AUDIO_ID$SEPARATOR${subtype[0]}"
                            Timber.d("Listing song of album $albumId")

                            getAlbumContainer(albumId, parentId)
                        }
                        subtype.size == 3 && subtype[0] == ALL_ARTISTS -> {
                            val albumId = subtype[2].toString()
                            val parentId =
                                "$AUDIO_ID$SEPARATOR${subtype[0]}$SEPARATOR${subtype[1]}"

                            Timber.d(
                                "Listing song of album %s for artist %s",
                                albumId,
                                subtype[1]
                            )

                            getAlbumContainer(albumId, parentId)
                        }
                        else -> throw noSuchObject
                    }

                    IMAGE_ID -> containerRegistry[subtype[0]] ?: throw noSuchObject
                    else -> containerRegistry[subtype[0]] ?: throw noSuchObject
                }
            }

            getBrowseResult(container)
        } catch (ex: Exception) {
            Timber.e(ex)
            throw ContentDirectoryException(
                ContentDirectoryErrorCode.CANNOT_PROCESS,
                ex.toString()
            )
        }
    }

    private fun getBrowseResult(container: BaseContainer): BrowseResult {
        Timber.d("List container...")

        val didl = DIDLContent().apply {
            listOf(
                LinkedHashSet(container.containers),
                LinkedHashSet(container.items)
            ).flatten().forEach { addObject(it) }
        }

        val count = didl.count

        Timber.d("Child count: $count")

        val answer: String

        try {
            answer = DIDLParser().generate(didl)
        } catch (ex: Exception) {
            throw ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, ex.toString())
        }

        return BrowseResult(answer, count, count)
    }

    private fun getRootImagesContainer(rootContainer: BaseContainer): BaseContainer =
        Container(
            IMAGE_ID.toString(),
            ROOT_ID.toString(),
            context.getString(R.string.images),
            appName
        ).apply {
            rootContainer.addContainer(this)

            AllImagesContainer(
                id = ALL_IMAGE.toString(),
                parentID = IMAGE_ID.toString(),
                title = context.getString(R.string.all),
                creator = appName,
                baseUrl = baseURL,
                contentResolver = context.contentResolver
            ).also { container ->
                addContainer(container)
                containerRegistry[ALL_IMAGE] = container
            }

            Container(
                IMAGE_BY_FOLDER.toString(),
                id,
                context.getString(R.string.by_folder),
                appName
            ).also { container ->
                addContainer(container)
                containerRegistry[IMAGE_BY_FOLDER] = container

                val result = FileHierarchyBuilder().populate(
                    contentResolver = context.contentResolver,
                    parentContainer = container,
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    column = ImageDirectoryContainer.IMAGE_DATA_PATH
                ) { id: String,
                    parentId: String?,
                    containerName: String,
                    path: String
                    ->

                    ImageDirectoryContainer(
                        id = id,
                        parentID = parentId ?: VIDEO_ID.toString(),
                        title = containerName,
                        creator = appName,
                        baseUrl = baseURL,
                        directory = ContentDirectory(path),
                        contentResolver = context.contentResolver
                    )
                }

                containerRegistry.putAll(result)
            }
        }

    private fun getRootAudioContainer(rootContainer: BaseContainer): BaseContainer =
        Container(
            AUDIO_ID.toString(),
            ROOT_ID.toString(),
            context.getString(R.string.audio),
            appName
        ).apply {
            rootContainer.addContainer(this)

            AllAudioContainer(
                ALL_AUDIO.toString(),
                AUDIO_ID.toString(),
                context.getString(R.string.all),
                appName,
                baseUrl = baseURL,
                contentResolver = context.contentResolver,
                albumId = null,
                artist = null
            ).also { container ->
                containerRegistry[ALL_AUDIO] = container
                addContainer(container)
            }

            ArtistContainer(
                ALL_ARTISTS.toString(),
                AUDIO_ID.toString(),
                context.getString(R.string.artist),
                appName,
                baseURL,
                context.contentResolver
            ).also { container ->
                containerRegistry[ALL_ARTISTS] = container
                addContainer(container)
            }

            AlbumContainer(
                ALL_ALBUMS.toString(),
                AUDIO_ID.toString(),
                context.getString(R.string.album),
                appName,
                baseURL,
                context.contentResolver,
                null
            ).also { container ->
                containerRegistry[ALL_ALBUMS] = container
                addContainer(container)
            }

            Container(
                AUDIO_BY_FOLDER.toString(),
                id,
                context.getString(R.string.by_folder),
                appName
            ).also { container ->
                addContainer(container)
                containerRegistry[AUDIO_BY_FOLDER] = container

                val result = FileHierarchyBuilder().populate(
                    contentResolver = context.contentResolver,
                    parentContainer = container,
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    column = AudioDirectoryContainer.AUDIO_DATA_PATH
                ) { id: String,
                    parentId: String?,
                    containerName: String,
                    path: String
                    ->

                    AudioDirectoryContainer(
                        id = id,
                        parentID = parentId ?: AUDIO_ID.toString(),
                        title = containerName,
                        creator = appName,
                        baseUrl = baseURL,
                        directory = ContentDirectory(path),
                        contentResolver = context.contentResolver
                    )
                }

                containerRegistry.putAll(result)
            }
        }

    private fun getRootVideoContainer(rootContainer: BaseContainer): BaseContainer =
        Container(
            VIDEO_ID.toString(),
            ROOT_ID.toString(),
            context.getString(R.string.videos),
            appName
        ).apply {
            rootContainer.addContainer(this)

            AllVideoContainer(
                ALL_VIDEO.toString(),
                VIDEO_ID.toString(),
                context.getString(R.string.all),
                appName,
                baseURL,
                contentResolver = context.contentResolver
            ).also { container ->
                addContainer(container)
                containerRegistry[ALL_VIDEO] = container
            }

            Container(
                VIDEO_BY_FOLDER.toString(),
                id,
                context.getString(R.string.by_folder),
                appName
            ).also { container ->
                addContainer(container)
                containerRegistry[VIDEO_BY_FOLDER] = container

                val result = FileHierarchyBuilder().populate(
                    contentResolver = context.contentResolver,
                    parentContainer = container,
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    column = VideoDirectoryContainer.VIDEO_DATA_PATH
                ) { id: String,
                    parentId: String?,
                    containerName: String,
                    directory: String
                    ->

                    VideoDirectoryContainer(
                        id,
                        parentId ?: VIDEO_ID.toString(),
                        containerName,
                        appName,
                        baseUrl = baseURL,
                        directory = ContentDirectory(directory),
                        contentResolver = context.contentResolver
                    )
                }

                containerRegistry.putAll(result)
            }
        }

    private fun getAlbumContainer(
        albumId: String,
        parentId: String
    ): BaseContainer = AllAudioContainer(
        albumId,
        parentId,
        "",
        appName,
        baseUrl = baseURL,
        contentResolver = context.contentResolver,
        albumId = albumId,
        artist = null
    )

    private val isImagesEnabled
        get() = sharedPref.getBoolean(CONTENT_DIRECTORY_IMAGE, true)

    private val isAudioEnabled
        get() = sharedPref.getBoolean(CONTENT_DIRECTORY_AUDIO, true)

    private val isVideoEnabled
        get() = sharedPref.getBoolean(CONTENT_DIRECTORY_VIDEO, true)

    companion object {
        const val SEPARATOR = '$'

        // Type
        const val ROOT_ID = 0
        const val IMAGE_ID = 1
        const val AUDIO_ID = 2
        const val VIDEO_ID = 3

        // Type subfolder
        const val ALL_IMAGE = 11
        const val ALL_VIDEO = 21
        const val ALL_AUDIO = 31

        const val IMAGE_BY_FOLDER = 12
        const val VIDEO_BY_FOLDER = 22
        const val AUDIO_BY_FOLDER = 32

        const val ALL_ARTISTS = 33
        const val ALL_ALBUMS = 34

        // Prefix item
        const val VIDEO_PREFIX = "v-"
        const val AUDIO_PREFIX = "a-"
        const val IMAGE_PREFIX = "i-"

        private val noSuchObject =
            ContentDirectoryException(ContentDirectoryErrorCode.NO_SUCH_OBJECT)

        fun isRoot(parentId: String?) =
            parentId?.compareTo(ROOT_ID.toString()) == 0
    }
}
