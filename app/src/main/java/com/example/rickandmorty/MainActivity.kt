package com.example.rickandmorty

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.rickandmorty.ui.theme.RickAndMortyTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import coil.compose.rememberImagePainter
import kotlinx.coroutines.Dispatchers
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.http.Path

interface RickAndMortyApi {
    @GET("character/{id}")
    suspend fun getCharacter(@Path("id") id: Int): CharacterResponse
}
data class CharacterResponse(
    val id: Int,
    val name: String,
    val image: String,
    val status: String,
    val species: String,
    val type: String,
    val gender: String,
    val origin: Origin,
    val location: Location
) {
    data class Origin(
        val name: String,
        val url: String
    )

    data class Location(
        val name: String,
        val url: String
    )
}

private val retrofit = Retrofit.Builder()
    .baseUrl("https://rickandmortyapi.com/api/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

private val apiService = retrofit.create(RickAndMortyApi::class.java)

@Composable
fun CharacterList(searchQuery: String, onCharacterClick: (CharacterResponse) -> Unit) {
    var charactersList by remember { mutableStateOf<List<CharacterResponse>>(emptyList()) }
    var filteredCharactersList by remember { mutableStateOf<List<CharacterResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(1) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val characters = fetchCharacters(1)
        charactersList = characters
        filteredCharactersList = characters
    }

    LaunchedEffect(searchQuery) {
        filteredCharactersList = if (searchQuery.isEmpty()) {
            charactersList
        } else {
            charactersList.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }



    LazyColumn (
        modifier = Modifier,
        state = listState
    ) {
        items(filteredCharactersList) { character ->
            ListItem(ListItemModel(imageRes = character.image, text = character.name),
                onClick = { onCharacterClick(character) })
        }
        item {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(

                    )
                }
            }
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .debounce(300)
            .collect { visibleItems ->
                val shouldLoadMore = visibleItems.lastOrNull()?.index == listState.layoutInfo.totalItemsCount - 1
                if (shouldLoadMore && !isLoading) {
                    coroutineScope.launch {
                        isLoading = true
                        page++
                        val newCharacters = fetchCharacters(page)
                        charactersList = charactersList + newCharacters
                        filteredCharactersList = if (searchQuery.isEmpty()) {
                            charactersList
                        } else {
                            charactersList.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        }
                        isLoading = false
                    }
                }
            }
    }

}

suspend fun fetchCharacters(page: Int): List<CharacterResponse> = withContext(Dispatchers.IO) {
    val characters = mutableListOf<CharacterResponse>()
    val startId = (page - 1) * 20 + 1
    val endId = page * 20
    for (id in startId..endId) {
        try {
            val character = apiService.getCharacter(id)
            characters.add(character)
        } catch (e: Exception) {
            Log.e("fetchCharacters", "Error fetching character with id: $id", e)
        }
    }
    return@withContext characters
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Displaying()
        }
    }

}


data class ListItemModel(val imageRes: String, val text: String)

@Composable
fun ListItem(item: ListItemModel, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(15.dp),
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 8.dp)
            ) {
                val painter: Painter = rememberImagePainter(data = item.imageRes)
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Text(text = item.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
    }
}


@Composable
fun Locations() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onTertiary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Locations here",
            fontWeight = FontWeight.Bold
        )
    }
}
@Composable
fun Episodes() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onTertiary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Episodes there",
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun Bookmarks() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onTertiary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Bookmarks here",
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BottomNavigationItem(text: String, isSelected: Boolean, icon: ImageVector, onClick: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f) else Color.Transparent
            )
            .padding(3.dp)
            .clickable { onClick(text) }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if(isSelected) Color.Blue else Color.Black

        )
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun BottomNavigation(currentScreen: String, onScreenSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    BottomAppBar(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomNavigationItem(
                text = "Locations",
                isSelected = currentScreen == "Locations",
                icon = Icons.Default.LocationOn,
                onClick = { onScreenSelected("Locations") }
            )
            BottomNavigationItem(
                text = "Characters",
                isSelected = currentScreen == "Characters",
                icon = Icons.Default.Person,
                onClick = { onScreenSelected("Characters") }
            )
            BottomNavigationItem(
                text = "Episodes",
                isSelected = currentScreen == "Episodes",
                icon = Icons.Default.PlayArrow,
                onClick = { onScreenSelected("Episodes") }
            )
            BottomNavigationItem(
                text = "Bookmarks",
                isSelected = currentScreen == "Bookmarks",
                icon = Icons.Default.Star,
                onClick = { onScreenSelected("Bookmarks") }
            )
        }
    }
}

@Composable
fun AppHeader(
    text: String,
    searchQuery: String,
    onQueryChanged: (String) -> Unit,
    isDetailView: Boolean,
    onBackButtonClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .height(60.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isDetailView) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackButtonClicked) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Character info",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(16.dp),
                )
            }
        } else {
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
                TextField(
                    value = searchQuery,
                    onValueChange = onQueryChanged,
                    placeholder = { Text(text = "Search") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true
                )
            }

    }
}

@Composable
fun Displaying() {
    var currentScreen by remember { mutableStateOf("Characters") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCharacter by remember { mutableStateOf<CharacterResponse?>(null) }

    RickAndMortyTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column {
                AppHeader(
                    text = currentScreen,
                    searchQuery = searchQuery,
                    onQueryChanged = { query -> searchQuery = query },
                    isDetailView = selectedCharacter != null,
                    onBackButtonClicked = {
                        selectedCharacter = null
                    }
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    when {
                        selectedCharacter != null -> CharacterDetail(selectedCharacter!!)
                        currentScreen == "Locations" -> Locations()
                        currentScreen == "Characters" -> CharacterList(searchQuery) { character ->
                            selectedCharacter = character
                        }
                        currentScreen == "Episodes" -> Episodes()
                        currentScreen == "Bookmarks" -> Bookmarks()
                    }
                }
                BottomNavigation(
                    currentScreen = currentScreen,
                    onScreenSelected = { selectedScreen -> currentScreen = selectedScreen
                        selectedCharacter = null },
                    modifier = Modifier.height(70.dp)
                )
            }
        }
    }
    if (selectedCharacter != null) {
        BackHandler {
            selectedCharacter = null
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Displaying()
}

@Preview(showBackground = true)
@Composable
fun DefaultPreviewCharacter() {
    CharacterDetail(CharacterResponse(1,"","","status","species","e","edw",CharacterResponse.Origin("",""), CharacterResponse.Location("","")))
}