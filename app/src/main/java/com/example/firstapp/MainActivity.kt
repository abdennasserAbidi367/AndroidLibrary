package com.example.firstapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.firstapp.ui.theme.FirstAppTheme
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.instagram.igdiskcache.EditorOutputStream
import com.instagram.igdiskcache.IgDiskCache
import com.instagram.igdiskcache.OptionalStream
import com.instagram.igdiskcache.SnapshotInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.net.URL

class MainActivity : ComponentActivity() {

    var igDiskCache: IgDiskCache? = null

    companion object {
        private const val DISK_CACHE_DIR = "bitmap"
        private const val DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 100 // 100MB
        private const val DEFAULT_DISK_CACHE_SIZE_PERCENT = 10f // 10% of free disk space
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (igDiskCache == null) {
            val cacheDir: File = Utils.getCacheDirectory(
                this,
                DISK_CACHE_DIR
            )

            CoroutineScope(Dispatchers.Default).launch {
                igDiskCache = IgDiskCache(
                    cacheDir,
                    Utils.getCacheSizeInBytes(
                        cacheDir,
                        DEFAULT_DISK_CACHE_SIZE_PERCENT / 100,
                        DEFAULT_DISK_CACHE_SIZE.toLong()
                    )
                )
            }
        }

        try {
            val url =
                URL("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
            val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
            igDiskCache?.let {
                addBitmapToCache(it, "alaaaa", bitmap)
            }
        } catch (e: IOException) {
            println(e)
        }

        /*igDiskCache?.let {
            getBitmapFromDiskCache(it, "alaaaa")
        }*/

        setContent {
            FirstAppTheme {
                NewApp()
            }
        }
    }
}

/**
 * Get the decoded Bitmap from disk cache
 */
fun getBitmapFromDiskCache(igDiskCache: IgDiskCache, key: String?): Bitmap? {
    var bitmap: Bitmap? = null
    val input: OptionalStream<SnapshotInputStream> = igDiskCache.get(key)
    if (input.isPresent) {
        try {
            val fd = input.get().fd
            bitmap = BitmapFactory.decodeFileDescriptor(fd)
        } catch (e: IOException) {
            Log.e("", "getBitmapFromDiskCache - $e")
        } finally {
            Utils.closeQuietly(input.get())
        }
    }
    return bitmap
}

/**
 * Flush the disk cache used for storing Bitmaps
 */
fun flush(igDiskCache: IgDiskCache) {
    igDiskCache.flush()
}

/**
 * Close the disk cache used for storing Bitmaps
 */
fun close(igDiskCache: IgDiskCache) {
    igDiskCache.close()
}

fun addBitmapToCache(igDiskCache: IgDiskCache, key: String?, bitmap: Bitmap?) {
    if (key == null || bitmap == null) {
        return
    }
    if (!igDiskCache.has(key)) {
        val output: OptionalStream<EditorOutputStream> = igDiskCache.edit(key)
        if (output.isPresent) {
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, output.get())
                output.get().commit()
            } catch (e: Exception) {
                Log.e(
                    "MainActivity",
                    "addBitmapToCache - $e"
                )
            } finally {
                output.get().abortUnlessCommitted()
            }
        }
    }
}

@Composable
fun NewApp() {
    var shouldShowOnboarding by rememberSaveable { mutableStateOf(true) }
    if (shouldShowOnboarding) OnboardingScreen(onContinueClicked = { shouldShowOnboarding = false })
    else MyApp()
    //else Greetings()
}

@Composable
fun MyPlayer() {
    val sampleVideo =
        "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    val context = LocalContext.current
    val player = SimpleExoPlayer.Builder(context).build()
    val playerView = PlayerView(context)
    val mediaItem = MediaItem.fromUri(sampleVideo)
    val playWhenReady by rememberSaveable {
        mutableStateOf(true)
    }
    player.setMediaItem(mediaItem)
    playerView.player = player
    LaunchedEffect(player) {
        player.prepare()
        player.playWhenReady = playWhenReady
    }
    AndroidView(factory = {
        playerView
    })
}

@Composable
fun Greeting(name: String) {
    val expended = remember { mutableStateOf(false) }
    val extraPadding by animateDpAsState(
        if (expended.value) 48.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    Surface(
        color = MaterialTheme.colors.primary,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Row(modifier = Modifier.padding(24.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = extraPadding.coerceAtLeast(0.dp))
            ) {
                Text(text = "Hello, ")
                Text(
                    text = name,
                    style = MaterialTheme.typography.h4.copy(
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                if (expended.value) {
                    Text(
                        text = ("Composem ipsum color sit lazy, " +
                                "padding theme elit, sed do bouncy. ").repeat(4),
                    )
                }
            }
            IconButton(onClick = { expended.value = !expended.value }) {
                Icon(
                    imageVector = if (expended.value) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expended.value) {
                        stringResource(R.string.show_less)
                    } else {
                        stringResource(R.string.show_more)
                    }
                )
            }
        }
    }
}

@Composable
fun FakeGreeting(ind: Int) {
    val names1: List<String> = List(1000) { "$it" }

    Surface(
        color = MaterialTheme.colors.primary,
        modifier = Modifier
            .height(450.dp)
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Row(modifier = Modifier.padding(24.dp)) {
            LazyRow {
                itemsIndexed(items = names1) { index, name ->
//                    MyPlayer()
                    if (ind.rem(2) == 0) {
                        if (index.rem(2) == 0) {
                            Image(
                                painterResource(R.drawable.ic_launcher_background),
                                "content description",
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(),
                                contentScale = ContentScale.Crop
                                //Modifier.height(450.dp).padding(end = 10.dp)
                            )
                        } else MyPlayer()
                    } else {
                        if (index.rem(2) == 0) MyPlayer()
                        else {
                            Image(
                                painterResource(R.drawable.ic_launcher_background),
                                "content description",
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(),
                                contentScale = ContentScale.Crop
                                //Modifier.height(450.dp).padding(end = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Greetings(names: List<String> = List(1000) { "$it" }) {
    LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
        itemsIndexed(items = names) { index, name ->
            FakeGreeting(index)
        }
    }
}

@Composable
fun OnboardingScreen(onContinueClicked: () -> Unit) {
    Surface {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome!")
            Button(
                modifier = Modifier.padding(vertical = 24.dp),
                onClick = onContinueClicked
            ) {
                Text("Continue")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 320)
@Composable
fun OnboardingPreview() {
    FirstAppTheme {
        OnboardingScreen(onContinueClicked = {})
    }
}

@Composable
fun MyApp(names: List<String> = listOf("World", "Compose")) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        for (name in names) {
            Greeting(name = name)
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun DefaultPreview() {
    FirstAppTheme {
        Greetings()
    }
}