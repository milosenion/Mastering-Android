# LazyColumn Complete Guide for Jetpack Compose

## Table of Contents
1. [What is LazyColumn?](#what-is-lazycolumn)
2. [LazyColumn vs Column](#lazycolumn-vs-column)
3. [Basic Usage](#basic-usage)
4. [Item Management](#item-management)
5. [Performance Optimization](#performance-optimization)
6. [Advanced Patterns](#advanced-patterns)
7. [Common Use Cases](#common-use-cases)

## What is LazyColumn?

LazyColumn is Jetpack Compose's equivalent to RecyclerView - it only composes and lays out visible items, making it efficient for large lists.

```kotlin
// How LazyColumn works internally:
┌─────────────────┐
│ Visible Area    │ ← Only these items are composed
│ ┌─────────────┐ │
│ │   Item 1    │ │
│ ├─────────────┤ │
│ │   Item 2    │ │
│ ├─────────────┤ │
│ │   Item 3    │ │
│ └─────────────┘ │
├─────────────────┤
│ Buffer Area     │ ← Pre-composed for smooth scrolling
│ ┌─────────────┐ │
│ │   Item 4    │ │
│ └─────────────┘ │
├─────────────────┤
│ Not Composed    │ ← Items 5-1000 not in memory
│ ...             │
└─────────────────┘
```

## LazyColumn vs Column

### Column - All items composed immediately:
```kotlin
Column {
    // ❌ Bad for large lists - all 1000 items composed!
    repeat(1000) { index ->
        Text("Item $index")
    }
}
```

### LazyColumn - Only visible items composed:
```kotlin
LazyColumn {
    // ✅ Good - only visible items composed
    items(1000) { index ->
        Text("Item $index")
    }
}
```

## Basic Usage

### Simple List:
```kotlin
@Composable
fun SimpleList(items: List<String>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            Card {
                Text(
                    text = item,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
```

### With Index:
```kotlin
LazyColumn {
    itemsIndexed(items) { index, item ->
        Row {
            Text("$index: ")
            Text(item)
        }
    }
}
```

## Item Management

### Different Ways to Add Items:

```kotlin
LazyColumn {
    // Single item
    item {
        HeaderSection()
    }
    
    // Multiple items with count
    items(
        count = 100,
        key = { index -> index } // Helps with recomposition
    ) { index ->
        ListItem(index)
    }
    
    // Items from list
    items(
        items = userList,
        key = { user -> user.id }, // Unique key for each item
        contentType = { user -> "user" } // Helps with recycling
    ) { user ->
        UserCard(user)
    }
    
    // Indexed items
    itemsIndexed(items = products) { index, product ->
        ProductRow(index, product)
    }
    
    // Multiple sections
    item { SectionHeader("Active Users") }
    items(activeUsers) { user -> UserItem(user) }
    
    item { SectionHeader("Inactive Users") }
    items(inactiveUsers) { user -> UserItem(user) }
}
```

### Keys and Content Types:

```kotlin
LazyColumn {
    items(
        items = messages,
        key = { message -> 
            // Unique, stable key for each item
            // Helps Compose track items during recomposition
            message.id 
        },
        contentType = { message ->
            // Type of content - helps with item recycling
            // Similar items can reuse compositions
            when (message) {
                is TextMessage -> "text"
                is ImageMessage -> "image"
                is VideoMessage -> "video"
            }
        }
    ) { message ->
        when (message) {
            is TextMessage -> TextMessageItem(message)
            is ImageMessage -> ImageMessageItem(message)
            is VideoMessage -> VideoMessageItem(message)
        }
    }
}
```

## Performance Optimization

### 1. Use Keys for Stable Identity:
```kotlin
// ❌ Bad - No keys
LazyColumn {
    items(users) { user ->
        UserItem(user)
    }
}

// ✅ Good - With keys
LazyColumn {
    items(
        items = users,
        key = { user -> user.id }
    ) { user ->
        UserItem(user)
    }
}
```

### 2. Avoid Heavy Computations:
```kotlin
// ❌ Bad - Computing in composition
@Composable
fun BadItem(data: ComplexData) {
    val processed = data.expensiveProcessing() // Runs on every recomposition!
    Text(processed)
}

// ✅ Good - Remember the result
@Composable
fun GoodItem(data: ComplexData) {
    val processed = remember(data) {
        data.expensiveProcessing()
    }
    Text(processed)
}
```

### 3. Use Stable Parameters:
```kotlin
// ❌ Bad - Unstable lambda
LazyColumn {
    items(items) { item ->
        ItemCard(
            item = item,
            onClick = { handleClick(item) } // New lambda each time!
        )
    }
}

// ✅ Good - Stable lambda
LazyColumn {
    items(items) { item ->
        ItemCard(
            item = item,
            onClick = remember { { handleClick(item) } }
        )
    }
}
```

## Advanced Patterns

### 1. Sticky Headers:
```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupedList(groupedItems: Map<String, List<Item>>) {
    LazyColumn {
        groupedItems.forEach { (header, items) ->
            stickyHeader {
                HeaderItem(header)
            }
            
            items(items) { item ->
                ItemRow(item)
            }
        }
    }
}
```

### 2. Pagination with Paging 3:
```kotlin
@Composable
fun PagingList(
    pagingItems: LazyPagingItems<User>
) {
    LazyColumn {
        items(
            count = pagingItems.itemCount,
            key = { index -> pagingItems[index]?.id ?: index }
        ) { index ->
            pagingItems[index]?.let { user ->
                UserItem(user)
            }
        }
        
        // Handle load states
        when (pagingItems.loadState.append) {
            is LoadState.Loading -> {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            }
            is LoadState.Error -> {
                item {
                    ErrorItem(
                        error = (pagingItems.loadState.append as LoadState.Error).error,
                        onRetry = { pagingItems.retry() }
                    )
                }
            }
            is LoadState.NotLoading -> Unit
        }
    }
}
```

### 3. Scroll State Management:
```kotlin
@Composable
fun ScrollableList() {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    Box {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(100) { index ->
                Text("Item $index")
            }
        }
        
        // Scroll to top button
        if (listState.firstVisibleItemIndex > 0) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Icon(Icons.Default.ArrowUpward, "Scroll to top")
            }
        }
    }
}
```

### 4. Different Item Types:
```kotlin
sealed class ListItem {
    data class Header(val title: String) : ListItem()
    data class User(val user: UserData) : ListItem()
    data class Ad(val adData: AdData) : ListItem()
    object Loading : ListItem()
}

@Composable
fun MixedContentList(items: List<ListItem>) {
    LazyColumn {
        items(
            items = items,
            key = { item ->
                when (item) {
                    is ListItem.Header -> "header_${item.title}"
                    is ListItem.User -> "user_${item.user.id}"
                    is ListItem.Ad -> "ad_${item.adData.id}"
                    is ListItem.Loading -> "loading"
                }
            },
            contentType = { item ->
                when (item) {
                    is ListItem.Header -> "header"
                    is ListItem.User -> "user"
                    is ListItem.Ad -> "ad"
                    is ListItem.Loading -> "loading"
                }
            }
        ) { item ->
            when (item) {
                is ListItem.Header -> HeaderItem(item.title)
                is ListItem.User -> UserItem(item.user)
                is ListItem.Ad -> AdItem(item.adData)
                is ListItem.Loading -> LoadingItem()
            }
        }
    }
}
```

## Common Use Cases

### 1. Pull to Refresh:
```kotlin
@Composable
fun RefreshableList(
    items: List<Item>,
    onRefresh: () -> Unit,
    isRefreshing: Boolean
) {
    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = onRefresh
    ) {
        LazyColumn {
            items(items) { item ->
                ItemCard(item)
            }
        }
    }
}
```

### 2. Infinite Scroll:
```kotlin
@Composable
fun InfiniteScrollList(
    items: List<Item>,
    onLoadMore: () -> Unit,
    isLoading: Boolean
) {
    val listState = rememberLazyListState()
    
    // Detect when to load more
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf false
            
            lastVisibleItem.index >= items.size - 5 // Load when 5 items from end
        }
    }
    
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoading) {
            onLoadMore()
        }
    }
    
    LazyColumn(state = listState) {
        items(items) { item ->
            ItemCard(item)
        }
        
        if (isLoading) {
            item {
                LoadingIndicator()
            }
        }
    }
}
```

### 3. Section List with Headers:
```kotlin
data class Section(
    val title: String,
    val items: List<Item>
)

@Composable
fun SectionedList(sections: List<Section>) {
    LazyColumn {
        sections.forEach { section ->
            item(key = "header_${section.title}") {
                SectionHeader(section.title)
            }
            
            items(
                items = section.items,
                key = { item -> "${section.title}_${item.id}" }
            ) { item ->
                ItemRow(item)
            }
        }
    }
}
```

### 4. Animated List:
```kotlin
@Composable
fun AnimatedList(items: List<Item>) {
    LazyColumn {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            AnimatedVisibility(
                visible = true,
                enter = slideInHorizontally() + fadeIn(),
                exit = slideOutHorizontally() + fadeOut()
            ) {
                ItemCard(item)
            }
        }
    }
}
```

## Best Practices

### 1. Always Use Keys:
```kotlin
// Helps Compose track items across recompositions
items(
    items = users,
    key = { user -> user.id }
) { user ->
    UserItem(user)
}
```

### 2. Optimize Item Composables:
```kotlin
// Keep items lightweight and focused
@Composable
fun OptimizedItem(item: Item) {
    // Use remember for expensive operations
    val processedData = remember(item.id) {
        processItem(item)
    }
    
    // Avoid unnecessary recompositions
    Card(
        modifier = Modifier.clickable {
            // Handle click
        }
    ) {
        Text(processedData)
    }
}
```

### 3. Handle Empty States:
```kotlin
if (items.isEmpty()) {
    EmptyStateView()
} else {
    LazyColumn {
        items(items) { item ->
            ItemView(item)
        }
    }
}
```

### 4. Use contentPadding Instead of Padding Modifier:
```kotlin
// ❌ Bad - Padding cuts off scrolling
LazyColumn(
    modifier = Modifier.padding(16.dp)
) {
    // Items
}

// ✅ Good - Content padding allows proper scrolling
LazyColumn(
    contentPadding = PaddingValues(16.dp)
) {
    // Items
}
```

## Summary

LazyColumn is essential for:
- Efficient list rendering
- Large data sets
- Dynamic content
- Smooth scrolling performance

Remember:
- Use keys for item stability
- Keep items lightweight
- Handle different states properly
- Optimize for performance