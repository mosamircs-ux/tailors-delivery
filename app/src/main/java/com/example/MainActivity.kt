package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.AppViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = viewModel()
            val themeMode by appViewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = darkTheme) {
                MainAppScreen(appViewModel = appViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(appViewModel: AppViewModel = viewModel()) {
    val currentUser by appViewModel.currentUser.collectAsState()
    val dbInitialized by appViewModel.dbInitialized.collectAsState()
    val userLanguage by appViewModel.userLanguage.collectAsState()

    // Screen State: "dashboard", "wizard", "orders", "profile", "ai_studio", "try_on", "marketplace", "map", "chat"
    var currentScreen by remember { mutableStateOf("dashboard") }
    var showNotifications by remember { mutableStateOf(false) }

    val notifications by appViewModel.currentNotifications.collectAsState()
    val unreadNotificationsCount = notifications.count { !it.isRead }

    if (!dbInitialized) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (userLanguage == "ar") "جاري تهيئة فاشن فورج بامتياز..." else "Initializing Fashion Forge...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        return
    }

    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(dbInitialized) {
        if (dbInitialized) {
            delay(1500)
            showSplash = false
        }
    }

    if (showSplash) {
        SplashScreenLayout(userLanguage = userLanguage)
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_main_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp)), // Sleek rounded corners
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                // Dynamic custom scissors logo
                                Icon(
                                    imageVector = Icons.Default.Casino, // scissor analog
                                    contentDescription = "Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = com.example.ui.Localization.get("app_title", userLanguage),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = com.example.ui.Localization.get("app_subtitle", userLanguage),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                actions = {
                    // Notification Icon with Badge
                    IconButton(
                        onClick = { showNotifications = !showNotifications },
                        modifier = Modifier.testTag("notifications_bell")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.testTag("notification_badge")
                                    ) {
                                        Text(unreadNotificationsCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alerts"
                            )
                        }
                    }

                    // Sleek Profile badge matching HTML: JD in bg-[#6750A4] with light-blue border
                    IconButton(
                        onClick = { currentScreen = "profile" },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                                .border(2.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser?.name?.split(" ")?.mapNotNull { it.firstOrNull() }?.take(2)?.joinToString("")?.uppercase() ?: "JD",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column {
                // Floating Role Portals Switcher
                PortalRoleSwitcher(
                    currentRole = currentUser?.role ?: "CUSTOMER",
                    userLanguage = userLanguage,
                    onRoleSelected = { role ->
                        appViewModel.switchUserRole(role)
                        currentScreen = "dashboard"
                    }
                )

                // Master Navigation Bar
                NavigationBar(
                    containerColor = Color(0xFFF3F4F9), // Sleek Bottom Navigation Background
                    windowInsets = WindowInsets.navigationBars
                ) {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001D35),
                        unselectedIconColor = Color(0xFF44474E),
                        selectedTextColor = Color(0xFF001D35),
                        unselectedTextColor = Color(0xFF44474E),
                        indicatorColor = Color(0xFFD3E3FD) // Sleek Selection Pill Background
                    )

                    NavigationBarItem(
                        selected = currentScreen == "dashboard" || currentScreen == "wizard" || currentScreen == "ai_studio" || currentScreen == "try_on",
                        onClick = { currentScreen = "dashboard" },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                        label = { Text(com.example.ui.Localization.get("nav_home", userLanguage)) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_home")
                    )
                    NavigationBarItem(
                        selected = currentScreen == "orders" || currentScreen == "chat",
                        onClick = { currentScreen = "orders" },
                        icon = { Icon(Icons.Default.List, contentDescription = "Orders") },
                        label = { Text(com.example.ui.Localization.get("nav_orders", userLanguage)) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_orders")
                    )
                    NavigationBarItem(
                        selected = currentScreen == "marketplace",
                        onClick = { currentScreen = "marketplace" },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Market") },
                        label = { Text(com.example.ui.Localization.get("nav_market", userLanguage)) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_market")
                    )
                    NavigationBarItem(
                        selected = currentScreen == "map",
                        onClick = { currentScreen = "map" },
                        icon = { Icon(Icons.Default.LocationOn, contentDescription = "Map") },
                        label = { Text(com.example.ui.Localization.get("nav_map", userLanguage)) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_map")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main body routing
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "screen_routing"
            ) { target ->
                when (target) {
                    "dashboard" -> {
                        when (currentUser?.role) {
                            "CUSTOMER" -> CustomerDashboard(
                                appViewModel = appViewModel,
                                onNavigate = { currentScreen = it }
                            )
                            "TAILOR" -> TailorDashboard(
                                appViewModel = appViewModel,
                                onNavigateToOrderDetails = { currentScreen = "orders" }
                            )
                            "SUPPLIER" -> SupplierPortal(appViewModel = appViewModel)
                            "DELIVERY" -> DeliveryPortal(appViewModel = appViewModel)
                        }
                    }
                    "wizard" -> CustomDesignWizard(
                        appViewModel = appViewModel,
                        onOrderCompleted = { currentScreen = "orders" }
                    )
                    "ai_studio" -> AIFashionStudio(
                        appViewModel = appViewModel,
                        onSubmittedToTailor = { currentScreen = "orders" }
                    )
                    "try_on" -> VirtualTryOn(appViewModel = appViewModel)
                    "marketplace" -> DesignerMarketplace(
                        appViewModel = appViewModel,
                        onCheckoutCompleted = { currentScreen = "orders" }
                    )
                    "orders" -> OrdersPortal(
                        appViewModel = appViewModel,
                        onOpenChat = { currentScreen = "chat" }
                    )
                    "chat" -> ChatPortal(
                        appViewModel = appViewModel,
                        onBack = { currentScreen = "orders" }
                    )
                    "map" -> InteractiveWorkshopMap(appViewModel = appViewModel)
                    "profile" -> UserProfileScreen(appViewModel = appViewModel)
                    "supplier_inventory" -> SupplierInventoryDashboard(
                        appViewModel = appViewModel,
                        onBack = { currentScreen = "dashboard" }
                    )
                }
            }

            // Notifications Dialog/Overlay
            if (showNotifications) {
                NotificationOverlay(
                    notifications = notifications,
                    onDismiss = { showNotifications = false },
                    onDismissAll = {
                        appViewModel.dismissNotifications()
                        showNotifications = false
                    },
                    onNotificationClick = { linkId ->
                        appViewModel.selectActiveChatOrder(linkId)
                        currentScreen = "orders"
                        showNotifications = false
                    }
                )
            }
        }
    }
}

// Global Portal Persona Switcher Bar
@Composable
fun PortalRoleSwitcher(
    currentRole: String,
    userLanguage: String,
    onRoleSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = com.example.ui.Localization.get("switch_portal", userLanguage),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End
        ) {
            val roles = if (userLanguage == "ar") {
                listOf(
                    "CUSTOMER" to "العميل",
                    "TAILOR" to "الترزي",
                    "SUPPLIER" to "المورد",
                    "DELIVERY" to "المندوب"
                )
            } else {
                listOf(
                    "CUSTOMER" to "Customer",
                    "TAILOR" to "Tailor",
                    "SUPPLIER" to "Supplier",
                    "DELIVERY" to "Delivery"
                )
            }
            roles.forEach { (roleKey, arLabel) ->
                val isSelected = currentRole == roleKey
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onRoleSelected(roleKey) }
                        .testTag("switch_to_$roleKey"),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                ) {
                    Text(
                        text = arLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

// Notification Hub Overlay
@Composable
fun NotificationOverlay(
    notifications: List<NotificationEntity>,
    onDismiss: () -> Unit,
    onDismissAll: () -> Unit,
    onNotificationClick: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismissAll, modifier = Modifier.testTag("dismiss_all_notif")) {
                Text("Clear All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Text(
                "Notifications Hub",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(modifier = Modifier.width(300.dp)) {
                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Empty",
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("All caught up!", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                        items(notifications) { notif ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onNotificationClick(notif.linkId) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (notif.isRead) MaterialTheme.colorScheme.surfaceVariant
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = notif.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = notif.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

// ----------------------------------------------------
// CUSTOMER PORTAL DASHBOARD
// ----------------------------------------------------
@Composable
fun CustomerDashboard(
    appViewModel: AppViewModel,
    onNavigate: (String) -> Unit
) {
    val orders by appViewModel.customerOrders.collectAsState()
    val activeOrders = orders.filter { it.status != "COMPLETED" }
    val userLanguage by appViewModel.userLanguage.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // "Sleek Interface" Home Greetings Layout
        item {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = com.example.ui.Localization.get("start_something", userLanguage),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = com.example.ui.Localization.get("extraordinary", userLanguage),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = com.example.ui.Localization.get("choose_template", userLanguage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Hero Card
        item {
            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = MaterialTheme.colorScheme.secondary
            val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(24.dp), // Sleek 3xl style rounded corner
                colors = CardDefaults.cardColors(containerColor = primaryColor)
            ) {
                Box {
                    // Background layout styling
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawPath(
                            path = Path().apply {
                                moveTo(size.width * 0.4f, 0f)
                                quadraticTo(
                                    size.width * 0.7f, size.height * 0.5f,
                                    size.width * 0.3f, size.height
                                )
                                lineTo(size.width, size.height)
                                lineTo(size.width, 0f)
                                close()
                            },
                            brush = Brush.linearGradient(
                                colors = listOf(secondaryColor, primaryContainerColor)
                            )
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Bespoke Fitting, Local Sourced",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اطلب تفصيل لبسك بالمليمتر ويوصلك لحد البيت",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
                        )
                    }
                }
            }
        }

        // Active Orders Tracker with Progress Tracking & Live Status Updates
        if (activeOrders.isNotEmpty()) {
            item {
                CustomOrderTrackingPanel(
                    appViewModel = appViewModel,
                    onNavigate = onNavigate
                )
            }
        }

        // Feature Grid
        item {
            Text(
                text = if (userLanguage == "ar") "جناح الموضة والابتكار" else "Personal Design Suite",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                FeatureButton(
                    title = if (userLanguage == "ar") "مساعد التفصيل الذكي" else "Interactive Design Wizard",
                    sub = if (userLanguage == "ar") "معالج تفصيل مخصص 4-خطوات" else "4-Step custom styling",
                    icon = Icons.Default.Add,
                    color = MaterialTheme.colorScheme.primary,
                    cardStyle = "primary_container", // Pre-built flow styled like Blank Canvas
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_wizard"),
                    onClick = { onNavigate("wizard") }
                )
                Spacer(modifier = Modifier.width(12.dp))
                FeatureButton(
                    title = if (userLanguage == "ar") "استوديو الذكاء الاصطناعي" else "AI Design Studio",
                    sub = if (userLanguage == "ar") "توليد تصاميم ملابس وبوتيك فوري" else "Bespoke fashion generator",
                    icon = Icons.Default.Casino,
                    color = MaterialTheme.colorScheme.secondary,
                    cardStyle = "secondary_container", // Styled like Quick Launch
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_ai_studio"),
                    onClick = { onNavigate("ai_studio") }
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                FeatureButton(
                    title = if (userLanguage == "ar") "التجربة الافتراضية" else "Virtual Try-On",
                    sub = if (userLanguage == "ar") "تجربة قياس الملابس بالهاتف" else "garment testing overlays",
                    icon = Icons.Default.Face,
                    color = Color(0xFF9B59B6),
                    cardStyle = "outline",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_try_on"),
                    onClick = { onNavigate("try_on") }
                )
                Spacer(modifier = Modifier.width(12.dp))
                FeatureButton(
                    title = if (userLanguage == "ar") "سوق كبار المصممين" else "Designer market",
                    sub = if (userLanguage == "ar") "أرقى تصاميم البوتيك الجاهزة" else "Ready boutique designs",
                    icon = Icons.Default.ShoppingCart,
                    color = Color(0xFF2ECC71),
                    cardStyle = "outline",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_marketplace"),
                    onClick = { onNavigate("marketplace") }
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                FeatureButton(
                    title = if (userLanguage == "ar") "خريطة الورش والتفصيل" else "Egyptian Crafts Map",
                    sub = if (userLanguage == "ar") "تحديد مواقع الترزية والخياطين" else "Find tailors & fabrics",
                    icon = Icons.Default.LocationOn,
                    color = Color(0xFFE74C3C),
                    cardStyle = "outline",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_map"),
                    onClick = { onNavigate("map") }
                )
                Spacer(modifier = Modifier.width(12.dp))
                FeatureButton(
                    title = if (userLanguage == "ar") "ملف المقاسات الخاص بي" else "My Sizing Profile",
                    sub = if (userLanguage == "ar") "طول الأكمام والياقة والكتف" else "Lengths and shoulder index",
                    icon = Icons.Default.Star,
                    color = Color(0xFFF1C40F),
                    cardStyle = "outline",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_sizing"),
                    onClick = { onNavigate("profile") }
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                FeatureButton(
                    title = if (userLanguage == "ar") "مخزون واستعلام الموردين" else "Supplier Inventory",
                    sub = if (userLanguage == "ar") "تصفح الأقمشة والتقييمات ومدة التوصيل" else "View stocks, ratings & delivery",
                    icon = Icons.Default.Layers,
                    color = Color(0xFF3498DB),
                    cardStyle = "outline",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_supplier_inventory"),
                    onClick = { onNavigate("supplier_inventory") }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun FeatureButton(
    title: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    cardStyle: String = "outline",
    onClick: () -> Unit
) {
    val containerColor = when (cardStyle) {
        "primary_container" -> MaterialTheme.colorScheme.primaryContainer
        "secondary_container" -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when (cardStyle) {
        "primary_container" -> MaterialTheme.colorScheme.onPrimaryContainer
        "secondary_container" -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = when (cardStyle) {
        "primary_container" -> Color.Transparent
        "secondary_container" -> Color.Transparent
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = modifier
            .height(118.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp), // Sleek rounded corners matching rounded-3xl
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (cardStyle == "outline") 0.dp else 4.dp),
        border = if (cardStyle == "outline") BorderStroke(1.dp, borderColor) else null
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = when (cardStyle) {
                    "primary_container" -> contentColor.copy(alpha = 0.15f)
                    "secondary_container" -> contentColor.copy(alpha = 0.15f)
                    else -> color.copy(alpha = 0.12f)
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "",
                        tint = when (cardStyle) {
                            "primary_container" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "secondary_container" -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> color
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = contentColor
                )
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ProgressStepsLine(status: String) {
    val steps = listOf("PENDING", "IN_PROGRESS", "READY", "OUT_FOR_DELIVERY", "COMPLETED")
    val currentIndex = steps.indexOf(status)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { idx, stepName ->
            val isDone = idx <= currentIndex
            val isCurrent = idx == currentIndex

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrent) MaterialTheme.colorScheme.primary
                        else if (isDone) Color(0xFF2ECC71)
                        else Color.LightGray
                    )
            )

            if (idx < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .background(
                            if (idx < currentIndex) Color(0xFF2ECC71)
                            else Color.LightGray
                        )
                )
            }
        }
    }
}

fun getCustomStageIndex(status: String): Int {
    return when (status) {
        "PENDING", "PROCESSING", "Processing" -> 0
        "IN_PROGRESS", "TAILORING", "Tailoring", "READY" -> 1
        "OUT_FOR_DELIVERY", "Out for Delivery" -> 2
        "COMPLETED", "Completed" -> 3
        else -> 0
    }
}

@Composable
fun CustomOrderProgressTracker(status: String, userLanguage: String) {
    val stageIndex = getCustomStageIndex(status)
    val stages = listOf(
        Pair(
            if (userLanguage == "ar") "تحت التجهيز" else "Processing",
            if (userLanguage == "ar") "تجهيز الأقمشة والمقاسات" else "Fabric & prep work"
        ),
        Pair(
            if (userLanguage == "ar") "الخياطة والتفصيل" else "Tailoring",
            if (userLanguage == "ar") "القص والتركيب والتفصيل" else "Stitching & designing"
        ),
        Pair(
            if (userLanguage == "ar") "خارج للتوصيل" else "Out for Delivery",
            if (userLanguage == "ar") "الطلب مع مندوب الشحن" else "En route to doorstep"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            stages.forEachIndexed { idx, stage ->
                val isActiveOrDone = idx <= stageIndex
                val isCurrent = idx == stageIndex
                val circleColor = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else if (isActiveOrDone) {
                    Color(0xFF2ECC71) // Nice Green
                } else {
                    Color.LightGray.copy(alpha = 0.6f)
                }

                val textColor = if (isActiveOrDone) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = circleColor,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val icon = when (idx) {
                                    0 -> Icons.Default.Add
                                    1 -> Icons.Default.Casino
                                    else -> Icons.Default.LocationOn
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = stage.first,
                                    tint = if (isActiveOrDone) Color.White else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = stage.first,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stage.second,
                        fontSize = 8.sp,
                        color = textColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 10.sp
                    )
                }

                if (idx < stages.size - 1) {
                    val lineColor = if (idx < stageIndex) Color(0xFF2ECC71) else Color.LightGray.copy(alpha = 0.5f)
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .height(3.dp)
                            .background(lineColor)
                            .align(Alignment.CenterVertically)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomOrderTrackingPanel(
    appViewModel: AppViewModel,
    onNavigate: (String) -> Unit
) {
    val orders by appViewModel.customerOrders.collectAsState()
    val activeOrders = orders.filter { it.status != "COMPLETED" }
    val userLanguage by appViewModel.userLanguage.collectAsState()

    if (activeOrders.isEmpty()) {
        return
    }

    var selectedOrderIndex by remember { mutableStateOf(0) }
    if (selectedOrderIndex >= activeOrders.size) {
        selectedOrderIndex = 0
    }
    
    val selectedOrder = activeOrders.getOrNull(selectedOrderIndex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("custom_order_tracking_panel")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (userLanguage == "ar") "تتبع الطلبات المميزة" else "Bespoke Order Tracker",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (userLanguage == "ar") "مراحل التصنيع اليدوي والتسليم" else "Crafting & delivery phases",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${activeOrders.size}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeOrders.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                activeOrders.forEachIndexed { index, order ->
                    val isSel = selectedOrderIndex == index
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedOrderIndex = index }
                            .testTag("track_sel_tab_$index"),
                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = order.orderNumber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (selectedOrder != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedOrder.category} • ${selectedOrder.templateName}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Surface(
                            color = getStatusColor(selectedOrder.status),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = selectedOrder.status,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Fabric: ${selectedOrder.fabricName} | Fit: ${selectedOrder.sizeType}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    CustomOrderProgressTracker(status = selectedOrder.status, userLanguage = userLanguage)

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (userLanguage == "ar") "تحديث الحالة يدويًا للتجربة:" else "Interactive Simulation Updates:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val updateStages = listOf(
                            Triple("PENDING", if (userLanguage == "ar") "التجهيز" else "Processing", Color(0xFF2C3E50)),
                            Triple("IN_PROGRESS", if (userLanguage == "ar") "التفصيل" else "Tailoring", Color(0xFFE67E22)),
                            Triple("OUT_FOR_DELIVERY", if (userLanguage == "ar") "شحن" else "Shipping", Color(0xFFF1C40F))
                        )

                        updateStages.forEach { (statusKey, labelText, btnCol) ->
                            val isActiveState = selectedOrder.status == statusKey
                            Button(
                                onClick = {
                                    appViewModel.updateOrderStatusByTailor(selectedOrder.id, statusKey)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .testTag("btn_track_set_$statusKey"),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isActiveState) btnCol else btnCol.copy(alpha = 0.15f),
                                    contentColor = if (isActiveState) Color.White else btnCol
                                )
                            ) {
                                Text(
                                    text = labelText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// CUSTOM DESIGN WIZARD
// ----------------------------------------------------
@Composable
fun CustomDesignWizard(
    appViewModel: AppViewModel,
    onOrderCompleted: () -> Unit
) {
    var step by remember { mutableStateOf(1) }

    // State parameters
    var selectedCategory by remember { mutableStateOf("Shirt") }
    var selectedTemplate by remember { mutableStateOf("Oxford Comfort Premium") }
    var selectedColorHex by remember { mutableStateOf("FFFFFF") }
    var selectedFabric by remember { mutableStateOf("Egyptian Cotton Premium") }
    var selectedNotes by remember { mutableStateOf("") }
    var sizeType by remember { mutableStateOf("M") }

    // Measurement Sizing Form Indices
    var lengthIndex by remember { mutableStateOf(72f) }
    var widthIndex by remember { mutableStateOf(52f) }
    var sleeveIndex by remember { mutableStateOf(62f) }
    var collarIndex by remember { mutableStateOf(40f) }

    // Delivery Form details
    var deliveryAddress by remember { mutableStateOf("5 El Maadi St, Cairo") }
    var deliveryNotes by remember { mutableStateOf("Call first") }
    var paymentMethod by remember { mutableStateOf("COD") } // "Card" or "COD"

    var cardNo by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvc by remember { mutableStateOf("") }

    val basePrice = when (selectedCategory) {
        "Shirt" -> 1450.0
        "Pants" -> 1600.0
        "Dress" -> 3200.0
        "Suit" -> 5200.0
        else -> 1200.0
    }

    val finalPrice = if (sizeType == "Custom") basePrice + 350.0 else basePrice

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step header indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Step $step of 4",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Row {
                repeat(4) { idx ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (idx + 1 == step) MaterialTheme.colorScheme.primary
                                else if (idx + 1 < step) Color(0xFF2ECC71)
                                else Color.LightGray
                            )
                    )
                }
            }
        }

        Divider()

        when (step) {
            1 -> {
                Text("Select Garment Category", style = MaterialTheme.typography.titleLarge)
                val categories = listOf(
                    "Shirt" to "قميص",
                    "Pants" to "بنطلون",
                    "Dress" to "فستان سهرة",
                    "Suit" to "بدلة كاملة",
                    "Jacket" to "جاكيت شتوي",
                    "Accessories" to "إكسسوار مكمل"
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories) { (cat, arabic) ->
                        val selected = selectedCategory == cat
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(94.dp)
                                .clickable {
                                    selectedCategory = cat
                                    selectedTemplate = when (cat) {
                                        "Shirt" -> "Oxford Comfort Premium"
                                        "Pants" -> "Classic Chino Tailor"
                                        "Dress" -> "A-Line Satin Silk"
                                        "Suit" -> "Al-Azhar Royal Signature"
                                        else -> "Contemporary Urban Style"
                                    }
                                }
                                .testTag("cat_$cat"),
                            border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(cat, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(arabic, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
            2 -> {
                Text("Bespoke Customization options", style = MaterialTheme.typography.titleLarge)

                // Color picker
                Text("Pick Garment Color Accent", fontWeight = FontWeight.Bold)
                val swatches = listOf(
                    "FFFFFF" to "White",
                    "2C3E50" to "Slate Navy",
                    "E67E22" to "Orange Amber",
                    "9E2A2B" to "Crimson Red",
                    "2E4053" to "Charcoal Dark",
                    "196F3D" to "Green Emerald",
                    "1F618D" to "Classic Blue",
                    "A04000" to "Clay Sienna"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    swatches.forEach { (hex, name) ->
                        val selected = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor("#$hex")))
                                .border(
                                    border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                                    else BorderStroke(1.dp, Color.LightGray),
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                                .testTag("color_$hex")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(13.dp))

                // Fabric choice
                Text("Choose Material Fabric", fontWeight = FontWeight.Bold)
                val fabricsList = listOf(
                    "Egyptian Cotton Premium",
                    "Italian Merino Wool",
                    "Belgian Pure Linen",
                    "Mulberry Premium Silk",
                    "Indigo Denim Standard"
                )
                fabricsList.forEach { fab ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFabric = fab }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedFabric == fab, onClick = { selectedFabric = fab })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(fab)
                    }
                }

                Spacer(modifier = Modifier.height(13.dp))

                OutlinedTextField(
                    value = selectedNotes,
                    onValueChange = { selectedNotes = it },
                    label = { Text("Special styling or lining requests...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            3 -> {
                Text("Sizing & Measurements Profile (مقاساتك)", style = MaterialTheme.typography.titleLarge)

                Row {
                    Button(
                        onClick = { sizeType = "M" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sizeType == "M") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Standard Size (M)", color = if (sizeType == "M") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { sizeType = "Custom" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sizeType == "Custom") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("size_custom")
                    ) {
                        Text("Bespoke Custom Sizes", color = if (sizeType == "Custom") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }

                if (sizeType == "Custom") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Double-check indices in centimeters (cm):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)

                    Text("Total Length Index: ${lengthIndex.toInt()} cm")
                    Slider(value = lengthIndex, onValueChange = { lengthIndex = it }, valueRange = 50f..150f)

                    Text("Chest Width Index: ${widthIndex.toInt()} cm")
                    Slider(value = widthIndex, onValueChange = { widthIndex = it }, valueRange = 40f..80f)

                    Text("Sleeve Length: ${sleeveIndex.toInt()} cm")
                    Slider(value = sleeveIndex, onValueChange = { sleeveIndex = it }, valueRange = 30f..80f)

                    Text("Collar Metric: ${collarIndex.toInt()} cm")
                    Slider(value = collarIndex, onValueChange = { collarIndex = it }, valueRange = 30f..55f)
                } else {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Standard Medium indices applied automatically on dispatch:", fontWeight = FontWeight.Bold)
                            Text("Length: 72cm | Chest: 52cm | Sleeve: 62cm | Collar: 40cm")
                        }
                    }
                }
            }
            4 -> {
                Text("Secure Checkout & Dispatch", style = MaterialTheme.typography.titleLarge)

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Order Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Garment: $selectedCategory ($selectedTemplate)")
                        Text("Material: $selectedFabric in Hex color #$selectedColorHex")
                        Text("Size Format: $sizeType Sizing Profile")
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Estimated EGP Price:")
                            Text("${finalPrice.toInt()} EGP", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                OutlinedTextField(
                    value = deliveryAddress,
                    onValueChange = { deliveryAddress = it },
                    label = { Text("Delivery Address (العنوان للتوصيل)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = deliveryNotes,
                    onValueChange = { deliveryNotes = it },
                    label = { Text("Delivery Instructions") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Choose Payment Gateway", fontWeight = FontWeight.Bold)
                Row {
                    Button(
                        onClick = { paymentMethod = "COD" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (paymentMethod == "COD") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cash on Delivery", color = if (paymentMethod == "COD") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { paymentMethod = "Card" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (paymentMethod == "Card") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("payment_card")
                    ) {
                        Text("Credit Card", color = if (paymentMethod == "Card") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }

                if (paymentMethod == "Card") {
                    OutlinedTextField(
                        value = cardNo,
                        onValueChange = { if (it.length <= 16) cardNo = it },
                        label = { Text("Card Number (16 Digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row {
                        OutlinedTextField(
                            value = cardExpiry,
                            onValueChange = { if (it.length <= 5) cardExpiry = it },
                            label = { Text("MM/YY") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = cardCvc,
                            onValueChange = { if (it.length <= 3) cardCvc = it },
                            label = { Text("CVC") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (step > 1) {
                OutlinedButton(onClick = { step-- }) { Text("Back") }
            } else {
                Spacer(modifier = Modifier.width(10.dp))
            }

            if (step < 4) {
                Button(onClick = { step++ }, modifier = Modifier.testTag("wizard_next")) { Text("Next") }
            } else {
                Button(
                    onClick = {
                        appViewModel.placeCustomOrder(
                            category = selectedCategory,
                            templateName = selectedTemplate,
                            colorHex = selectedColorHex,
                            fabricName = selectedFabric,
                            notes = selectedNotes,
                            sizeType = sizeType,
                            length = lengthIndex,
                            width = widthIndex,
                            sleeve = sleeveIndex,
                            collar = collarIndex,
                            deliveryAddress = deliveryAddress,
                            deliveryNotes = deliveryNotes,
                            paymentMethod = paymentMethod,
                            price = finalPrice
                        )
                        onOrderCompleted()
                    },
                    modifier = Modifier.testTag("place_custom_order")
                ) {
                    Text("Place Order & Pay (${finalPrice.toInt()} EGP)")
                }
            }
        }
    }
}

// Helper to get status color
fun getStatusColor(status: String): Color {
    return when (status) {
        "PENDING" -> NavyPrimary
        "IN_PROGRESS" -> OrangeSecondary
        "READY" -> BlueAccent
        "OUT_FOR_DELIVERY" -> Color(0xFFF1C40F)
        "COMPLETED" -> Color(0xFF2ECC71)
        else -> Color.Gray
    }
}

val NavyPrimary = Color(0xFF2C3E50)
val OrangeSecondary = Color(0xFFE67E22)
val BlueAccent = Color(0xFF3498DB)

// ----------------------------------------------------
// AI DESIGN STUDIO (GEMINI POWERED)
// ----------------------------------------------------
@Composable
fun BespokeSketchCanvas(
    result: com.example.network.AIDesignResult,
    modifier: Modifier = Modifier
) {
    val garmentType = result.garmentType ?: "shirt"
    val primaryColor = try { Color(android.graphics.Color.parseColor(result.primaryColorHex ?: "#2C3E50")) } catch(e: Exception) { Color(0xFF2C3E50) }
    val secondaryColor = try { Color(android.graphics.Color.parseColor(result.secondaryColorHex ?: "#E67E22")) } catch(e: Exception) { Color(0xFFE67E22) }
    val sleeveStyle = result.sleeveStyle ?: "long"
    val necklineStyle = result.necklineStyle ?: "round"
    val accents = result.designAccents ?: emptyList()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9F6F0)) // Premium Parchment Paper color
            .border(1.dp, Color(0xFFE0D8CB), RoundedCornerShape(12.dp))
            .testTag("bespoke_sketch_canvas")
    ) {
        // Draw the custom fashion blueprint sketching sheet
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw Tailor Grid guidelines
            val gridSpacing = 20.dp.toPx()
            // Vertical grid lines
            var x = 0f
            while (x < width) {
                drawLine(
                    color = Color(0xFFEAE5D9),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += gridSpacing
            }
            // Horizontal grid lines
            var y = 0f
            while (y < height) {
                drawLine(
                    color = Color(0xFFEAE5D9),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                y += gridSpacing
            }

            // 2. Draw Measurement Rules on edges
            // Top Ruler tick marks
            for (tick in 0..width.toInt() step 30) {
                val tickLen = if (tick % 90 == 0) 15f else 8f
                drawLine(
                    color = Color(0xFFB0A595),
                    start = Offset(tick.toFloat(), 0f),
                    end = Offset(tick.toFloat(), tickLen),
                    strokeWidth = 1.5f
                )
            }
            // Left Ruler tick marks
            for (tick in 0..height.toInt() step 30) {
                val tickLen = if (tick % 90 == 0) 15f else 8f
                drawLine(
                    color = Color(0xFFB0A595),
                    start = Offset(0f, tick.toFloat()),
                    end = Offset(tickLen, tick.toFloat()),
                    strokeWidth = 1.5f
                )
            }

            // Draw center axes guidelines (dotted)
            val centerAxisX = width / 2
            val centerAxisY = height / 2 + 10.dp.toPx()

            // Draw technical cross-hairs & waist-line guides
            drawLine(
                color = Color(0xFFC4BBAF),
                start = Offset(centerAxisX, 20f),
                end = Offset(centerAxisX, height - 20f),
                strokeWidth = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            // Waist line
            drawLine(
                color = Color(0xFFC4BBAF),
                start = Offset(40f, centerAxisY),
                end = Offset(width - 40f, centerAxisY),
                strokeWidth = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // Let's draw the Garment Outline Sketch automatically!
            // We configure the drawing coordinates around centerAxisX, centerAxisY
            val baseWidth = 70.dp.toPx()
            val waistWidth = 60.dp.toPx()
            val shoulderWidth = 80.dp.toPx()
            val topY = centerAxisY - 90.dp.toPx()
            val hemY = centerAxisY + 90.dp.toPx()

            val garmentPath = Path()
            
            // Draw garment depending on garmentType
            when (garmentType) {
                "dress" -> {
                    // Flare out dress hem
                    val flareHemWidth = 120.dp.toPx()
                    garmentPath.moveTo(centerAxisX - shoulderWidth / 2, topY) // Top Left Shoulder
                    garmentPath.lineTo(centerAxisX + shoulderWidth / 2, topY) // Top Right Shoulder
                    garmentPath.lineTo(centerAxisX + waistWidth / 2, centerAxisY) // Waist Right
                    garmentPath.lineTo(centerAxisX + flareHemWidth / 2, hemY) // Flare Hem Right
                    garmentPath.lineTo(centerAxisX - flareHemWidth / 2, hemY) // Flare Hem Left
                    garmentPath.lineTo(centerAxisX - waistWidth / 2, centerAxisY) // Waist Left
                    garmentPath.close()
                }
                "jacket", "suit" -> {
                    // Structured jacket silhouette
                    garmentPath.moveTo(centerAxisX - shoulderWidth / 2, topY)
                    garmentPath.lineTo(centerAxisX + shoulderWidth / 2, topY)
                    garmentPath.lineTo(centerAxisX + baseWidth / 2, centerAxisY)
                    garmentPath.lineTo(centerAxisX + baseWidth / 2, hemY - 20.dp.toPx())
                    garmentPath.lineTo(centerAxisX - baseWidth / 2, hemY - 20.dp.toPx())
                    garmentPath.lineTo(centerAxisX - baseWidth / 2, centerAxisY)
                    garmentPath.close()
                }
                "kaftan" -> {
                    // Flowing robe silhouette (Very long and wide)
                    val robeHemWidth = 110.dp.toPx()
                    garmentPath.moveTo(centerAxisX - shoulderWidth / 2, topY)
                    garmentPath.lineTo(centerAxisX + shoulderWidth / 2, topY)
                    garmentPath.lineTo(centerAxisX + robeHemWidth / 2, centerAxisY)
                    garmentPath.lineTo(centerAxisX + robeHemWidth / 2, hemY + 15.dp.toPx())
                    garmentPath.lineTo(centerAxisX - robeHemWidth / 2, hemY + 15.dp.toPx())
                    garmentPath.lineTo(centerAxisX - robeHemWidth / 2, centerAxisY)
                    garmentPath.close()
                }
                else -> { // "shirt"
                    // Standard shirt silhouette
                    garmentPath.moveTo(centerAxisX - shoulderWidth / 2, topY)
                    garmentPath.lineTo(centerAxisX + shoulderWidth / 2, topY)
                    garmentPath.lineTo(centerAxisX + baseWidth / 2, centerAxisY)
                    garmentPath.lineTo(centerAxisX + baseWidth / 2, hemY - 30.dp.toPx())
                    garmentPath.lineTo(centerAxisX - baseWidth / 2, hemY - 30.dp.toPx())
                    garmentPath.lineTo(centerAxisX - baseWidth / 2, centerAxisY)
                    garmentPath.close()
                }
            }

            // Fill Garment with the chosen primaryColor at low opacity for sketch fill
            drawPath(
                path = garmentPath,
                color = primaryColor.copy(alpha = 0.3f)
            )

            // Draw Outer seams/lines
            drawPath(
                path = garmentPath,
                color = primaryColor,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )

            // 3. Draw Sleeves
            if (sleeveStyle != "sleeveless") {
                val outerSleeveLen = if (sleeveStyle == "short") 35.dp.toPx() else 95.dp.toPx()
                val sleeveCuffY = topY + outerSleeveLen
                
                // Left Sleeve
                val leftSleevePath = Path().apply {
                    moveTo(centerAxisX - shoulderWidth / 2, topY)
                    lineTo(centerAxisX - shoulderWidth / 2 - 25.dp.toPx(), topY + 15.dp.toPx()) // Shoulder slope
                    lineTo(centerAxisX - shoulderWidth / 2 - 20.dp.toPx(), sleeveCuffY) // Cuff
                    lineTo(centerAxisX - shoulderWidth / 2 + 5.dp.toPx(), topY + 30.dp.toPx()) // Armpit
                    close()
                }
                drawPath(path = leftSleevePath, color = primaryColor.copy(alpha = 0.25f))
                drawPath(path = leftSleevePath, color = primaryColor, style = Stroke(width = 2.5f.dp.toPx()))

                // Right Sleeve
                val rightSleevePath = Path().apply {
                    moveTo(centerAxisX + shoulderWidth / 2, topY)
                    lineTo(centerAxisX + shoulderWidth / 2 + 25.dp.toPx(), topY + 15.dp.toPx()) // Shoulder slope
                    lineTo(centerAxisX + shoulderWidth / 2 + 20.dp.toPx(), sleeveCuffY) // Cuff
                    lineTo(centerAxisX + shoulderWidth / 2 - 5.dp.toPx(), topY + 30.dp.toPx()) // Armpit
                    close()
                }
                drawPath(path = rightSleevePath, color = primaryColor.copy(alpha = 0.25f))
                drawPath(path = rightSleevePath, color = primaryColor, style = Stroke(width = 2.5f.dp.toPx()))
            }

            // 4. Draw Neckline/Collar
            val neckPath = Path()
            val collarRadius = 16.dp.toPx()
            when (necklineStyle) {
                "v-neck" -> {
                    neckPath.moveTo(centerAxisX - collarRadius, topY)
                    neckPath.lineTo(centerAxisX, topY + collarRadius + 5f)
                    neckPath.lineTo(centerAxisX + collarRadius, topY)
                }
                "high-collar" -> {
                    // Draw a standing collar box
                    neckPath.moveTo(centerAxisX - collarRadius, topY)
                    neckPath.lineTo(centerAxisX - collarRadius, topY - 12.dp.toPx())
                    neckPath.lineTo(centerAxisX + collarRadius, topY - 12.dp.toPx())
                    neckPath.lineTo(centerAxisX + collarRadius, topY)
                    neckPath.close()
                }
                "lapel" -> {
                    // Double lines representing suit lapels crossing
                    neckPath.moveTo(centerAxisX - collarRadius, topY)
                    neckPath.lineTo(centerAxisX - collarRadius + 5f, topY + collarRadius)
                    neckPath.lineTo(centerAxisX, topY + 2.5f * collarRadius)
                    neckPath.lineTo(centerAxisX + collarRadius - 5f, topY + collarRadius)
                    neckPath.lineTo(centerAxisX + collarRadius, topY)
                }
                else -> { // "round"
                    neckPath.moveTo(centerAxisX - collarRadius, topY)
                    neckPath.cubicTo(
                        centerAxisX - collarRadius / 2, topY + collarRadius / 1.5f,
                        centerAxisX + collarRadius / 2, topY + collarRadius / 1.5f,
                        centerAxisX + collarRadius, topY
                    )
                }
            }
            drawPath(path = neckPath, color = secondaryColor, style = Stroke(width = 2.5f.dp.toPx()))

            // 5. Draw decorative accents on the sketch
            if (garmentType in listOf("shirt", "jacket", "suit", "kaftan")) {
                val buttonStartY = topY + 25.dp.toPx()
                val buttonEndY = hemY - 40.dp.toPx()
                val step = 18.dp.toPx()
                var currentButtonY = buttonStartY
                while (currentButtonY <= buttonEndY) {
                    drawCircle(
                        color = secondaryColor,
                        radius = 3.dp.toPx(),
                        center = Offset(centerAxisX, currentButtonY)
                    )
                    currentButtonY += step
                }
            }

            if (garmentType == "suit" || garmentType == "jacket") {
                val pocketX = centerAxisX - 25.dp.toPx()
                val pocketY = topY + 30.dp.toPx()
                drawRect(
                    color = secondaryColor.copy(alpha = 0.2f),
                    topLeft = Offset(pocketX, pocketY),
                    size = androidx.compose.ui.geometry.Size(14.dp.toPx(), 12.dp.toPx())
                )
                drawRect(
                    color = secondaryColor,
                    topLeft = Offset(pocketX, pocketY),
                    size = androidx.compose.ui.geometry.Size(14.dp.toPx(), 12.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Annotation leaders
            val arrowLeftStart = Offset(centerAxisX - 45.dp.toPx(), topY + 45.dp.toPx())
            val arrowLeftEnd = Offset(25.dp.toPx(), topY + 15.dp.toPx())
            drawLine(
                color = Color(0xFF7F8C8D),
                start = arrowLeftStart,
                end = arrowLeftEnd,
                strokeWidth = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
            )
            drawCircle(color = Color(0xFF7F8C8D), radius = 2.dp.toPx(), center = arrowLeftStart)

            val arrowRightStart = Offset(centerAxisX + 35.dp.toPx(), centerAxisY + 30.dp.toPx())
            val arrowRightEnd = Offset(width - 30.dp.toPx(), centerAxisY + 70.dp.toPx())
            drawLine(
                color = Color(0xFF7F8C8D),
                start = arrowRightStart,
                end = arrowRightEnd,
                strokeWidth = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
            )
            drawCircle(color = Color(0xFF7F8C8D), radius = 2.dp.toPx(), center = arrowRightStart)
        }

        Text(
            text = "SPEC. STYLE: ${necklineStyle.uppercase()} / ${sleeveStyle.uppercase()}",
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontSize = 9.sp,
            color = Color(0xFF7F8C8D),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        )

        val accentText = accents.take(2).joinToString(" • ")
        if (accentText.isNotEmpty()) {
            Text(
                text = "ACCENTS: $accentText",
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 8.5.sp,
                color = Color(0xFF7F8C8D),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            )
        }

        Text(
            text = "DRAFT BLUEPRINT SCALE 1:10",
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF95A5A6),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
        )
    }
}

// ----------------------------------------------------
// PREMIUM FABRICS & SCHEMES STRUCTURES
// ----------------------------------------------------
data class PremiumMaterial(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val detailsEn: String,
    val detailsAr: String,
    val priceEgp: Int,
    val threadInfoEn: String,
    val threadInfoAr: String
)

data class PremiumColor(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val hexValue: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaterialSelectionComponent(
    userLanguage: String,
    selectedMaterial: PremiumMaterial,
    onMaterialSelected: (PremiumMaterial) -> Unit,
    selectedColor: PremiumColor,
    onColorSelected: (PremiumColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val premiumMaterials = remember {
        listOf(
            PremiumMaterial(
                id = "giza_cotton",
                nameEn = "Pure Egyptian Giza Cotton",
                nameAr = "قطن جيزة المصري الفاخر",
                detailsEn = "Super-long cotton fiber known for ultimate strength, silk-like luster, and exquisite breathability. Perfect for bespoke luxury shirts & kaftans.",
                detailsAr = "ألياف قطنية فائقة الطول تشتهر بقوتها العالية، ولمعانها الحريري، وتهويتها الرائعة. مثالية للقمصان الفاخرة والقواطين.",
                priceEgp = 450,
                threadInfoEn = "120/2 Double Ply Weave",
                threadInfoAr = "حياكة مزدوجة الطبقات ١٢٠/٢"
            ),
            PremiumMaterial(
                id = "italian_linen",
                nameEn = "Royal Italian Linen",
                nameAr = "كتان إيطالي ملكي",
                detailsEn = "Pre-washed flax linen offering an incredibly soft handfeel and a gorgeous relaxed drape. Superior thermoregulation.",
                detailsAr = "كتان مغسول مسبقًا يوفر ملمسًا ناعمًا ومظهرًا مسترخيًا جذابًا. تنظيم حراري ممتاز لتناسب كل الفصول.",
                priceEgp = 380,
                threadInfoEn = "Pure Bio-Washed Flax",
                threadInfoAr = "كتان نقي بيولوجي مغسول"
            ),
            PremiumMaterial(
                id = "mulberry_silk",
                nameEn = "Mulberry Silk Blend",
                nameAr = "حرير التوت الطبيعي المختلط",
                detailsEn = "Satin silk with a highly reflective surface and graceful fluid drapery. Ideal for custom couture gowns & luxury robes.",
                detailsAr = "حرير التوت الطبيعي اللامع، ذو ذيل منسدل رشيق وانسيابية عالية. مثالي لفساتين السهرة والعباءات الراقية.",
                priceEgp = 650,
                threadInfoEn = "100% Organic Satin Silk",
                threadInfoAr = "حرير ساتان طبيعي ١٠٠٪"
            ),
            PremiumMaterial(
                id = "cashmere_wool",
                nameEn = "Bespoke Cashmere Wool",
                nameAr = "صوف كشمير مخصص",
                detailsEn = "Finely spun high-micron wool fibers. Exceptionally light yet provides unparalleled insulation and premium structured tailoring.",
                detailsAr = "ألياف صوف كشمير ناعمة. خبيت مخصص رقيق وسوبر ١٤٠ دقيق، وهيكل متقن للبدل الفاخرة.",
                priceEgp = 800,
                threadInfoEn = "Super 140s Cashmere Thread",
                threadInfoAr = "خيط صوف كشمير سوبر ١٤٠"
            ),
            PremiumMaterial(
                id = "brocade_jacquard",
                nameEn = "Damask Brocade Jacquard",
                nameAr = "حرير دمشقي جاكار",
                detailsEn = "Intricate raised metallic and gold patterns woven into heavy rich silk. Retains stiff elegant shape lines and premium tailor lines.",
                detailsAr = "نقوش كلاسيكية بارزة بخيوط مذهبة منسوجة مع الحرير الثقيل والفاخر. يحافظ على خطوط تفصيل أنيقة وبارزة.",
                priceEgp = 720,
                threadInfoEn = "Woven Ornate Jacquard",
                threadInfoAr = "جاكار مزخرف منسوج"
            )
        )
    }

    val premiumColors = remember {
        listOf(
            PremiumColor("emerald", "Emerald Dynasty", "زمرد ملكي", "#1E5E3A"),
            PremiumColor("indigo", "Midnight Indigo", "كحلي داكن", "#2B4C7E"),
            PremiumColor("burgundy", "Royal Burgundy", "خمري فاخر", "#8B1E1E"),
            PremiumColor("gold", "Sovereign Gold", "ذهبي ملكي", "#D4AF37"),
            PremiumColor("parchment", "Warm Parchment", "أوف وايت ناعم", "#F4F0E6"),
            PremiumColor("obsidian", "Obsidian Black", "أسود فحم", "#1A1A1A"),
            PremiumColor("purple", "Imperial Purple", "بنفسجي إمبريالي", "#6D214F"),
            PremiumColor("ochre", "Burnt Ochre", "أوكر برتقالي كلاسيك", "#D35400")
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Color Selection Section
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (userLanguage == "ar") "اختر لون القماش الفاخر:" else "Select Premium Fabric Color:",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            
            // Horizontal scrolling row of colors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                premiumColors.forEach { colorPreset ->
                    val isSelected = colorPreset.id == selectedColor.id
                    val colorNative = try {
                        Color(android.graphics.Color.parseColor(colorPreset.hexValue))
                    } catch (e: Exception) {
                        Color.Gray
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onColorSelected(colorPreset) }
                            .testTag("color_chip_${colorPreset.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(colorNative)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (colorPreset.hexValue == "#F4F0E6") Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (userLanguage == "ar") colorPreset.nameAr else colorPreset.nameEn,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // 2. Fabric Material Selection Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (userLanguage == "ar") "تصفّح واختر نوع القماش والوزن:" else "Browse & Select Custom Tailoring Fabric:",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            // Scrollable list of cards for premium fabrics
            premiumMaterials.forEach { material ->
                val isSelected = material.id == selectedMaterial.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMaterialSelected(material) }
                        .testTag("fabric_card_${material.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) 
                            else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (userLanguage == "ar") material.nameAr else material.nameEn,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            // Rounded Price Tag Badge
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.testTag("price_badge_${material.id}")
                            ) {
                                Text(
                                    text = "${material.priceEgp} EGP/m",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Thread Count or Weave style
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = Color(0xFFF1C40F),
                                modifier = Modifier.size(14.dp)
                              )
                            Text(
                                text = if (userLanguage == "ar") material.threadInfoAr else material.threadInfoEn,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = if (userLanguage == "ar") material.detailsAr else material.detailsEn,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AIFashionStudio(
    appViewModel: AppViewModel,
    onSubmittedToTailor: () -> Unit
) {
    val userLanguage by appViewModel.userLanguage.collectAsState()
    
    var studioModeTab by remember { mutableStateOf(0) } // 0 = Freeform Prompt, 1 = Guided Occasion
    var garmentPrompt by remember { mutableStateOf("") }

    var occasion by remember { mutableStateOf("Formal wedding") }
    var style by remember { mutableStateOf("Elegant") }
    var bodyType by remember { mutableStateOf("Athletic Frame") }
    var budgetStr by remember { mutableStateOf("3000") }

    val aiLoading by appViewModel.aiGenerationLoading.collectAsState()
    val aiResult by appViewModel.aiGenResult.collectAsState()

    // ----------------------------------------------------
    // PREMIUM MATERIAL & COLOR SCHEMES DEFINITION
    // ----------------------------------------------------
    val premiumMaterialsList = remember {
        listOf(
            PremiumMaterial(
                id = "giza_cotton",
                nameEn = "Pure Egyptian Giza Cotton",
                nameAr = "قطن جيزة المصري الفاخر",
                detailsEn = "Super-long cotton fiber known for ultimate strength, silk-like luster, and exquisite breathability. Perfect for bespoke luxury shirts & kaftans.",
                detailsAr = "ألياف قطنية فائقة الطول تشتهر بقوتها العالية، ولمعانها الحريري، وتهويتها الرائعة. مثالية للقمصان الفاخرة والقواطين.",
                priceEgp = 450,
                threadInfoEn = "120/2 Double Ply Weave",
                threadInfoAr = "حياكة مزدوجة الطبقات ١٢٠/٢"
            ),
            PremiumMaterial(
                id = "italian_linen",
                nameEn = "Royal Italian Linen",
                nameAr = "كتان إيطالي ملكي",
                detailsEn = "Pre-washed flax linen offering an incredibly soft handfeel and a gorgeous relaxed drape. Superior thermoregulation.",
                detailsAr = "كتان مغسول مسبقًا يوفر ملمسًا ناعمًا ومظهرًا مسترخيًا جذابًا. تنظيم حراري ممتاز لتناسب كل الفصول.",
                priceEgp = 380,
                threadInfoEn = "Pure Bio-Washed Flax",
                threadInfoAr = "كتان نقي بيولوجي مغسول"
            ),
            PremiumMaterial(
                id = "mulberry_silk",
                nameEn = "Mulberry Silk Blend",
                nameAr = "حرير التوت الطبيعي المختلط",
                detailsEn = "Satin silk with a highly reflective surface and graceful fluid drapery. Ideal for custom couture gowns & luxury robes.",
                detailsAr = "حرير التوت الطبيعي اللامع، ذو ذيل منسدل رشيق وانسيابية عالية. مثالي لفساتين السهرة والعباءات الراقية.",
                priceEgp = 650,
                threadInfoEn = "100% Organic Satin Silk",
                threadInfoAr = "حرير ساتان طبيعي ١٠٠٪"
            ),
            PremiumMaterial(
                id = "cashmere_wool",
                nameEn = "Bespoke Cashmere Wool",
                nameAr = "صوف كشمير مخصص",
                detailsEn = "Finely spun high-micron wool fibers. Exceptionally light yet provides unparalleled insulation and premium structured tailoring.",
                detailsAr = "ألياف صوف كشمير ناعمة. خفيف الوزن بشكل استثنائي ومع ذلك يوفر عزل حراري مثالي وهيكل متين للبدل الفاخرة.",
                priceEgp = 800,
                threadInfoEn = "Super 140s Cashmere Thread",
                threadInfoAr = "خيط صوف كشمير سوبر ١٤٠"
            ),
            PremiumMaterial(
                id = "brocade_jacquard",
                nameEn = "Damask Brocade Jacquard",
                nameAr = "حرير دمشقي جاكار",
                detailsEn = "Intricate raised metallic and gold patterns woven into heavy rich silk. Retains stiff elegant shape lines and premium tailor lines.",
                detailsAr = "نقوش كلاسيكية بارزة بخيوط مذهبة منسوجة مع الحرير الثقيل والفاخر. يحافظ على خطوط تفصيل أنيقة وبارزة.",
                priceEgp = 720,
                threadInfoEn = "Woven Ornate Jacquard",
                threadInfoAr = "جاكار مزخرف منسوج"
            )
        )
    }

    val premiumColorsList = remember {
        listOf(
            PremiumColor("emerald", "Emerald Dynasty", "زمرد ملكي", "#1E5E3A"),
            PremiumColor("indigo", "Midnight Indigo", "كحلي داكن", "#2B4C7E"),
            PremiumColor("burgundy", "Royal Burgundy", "خمري فاخر", "#8B1E1E"),
            PremiumColor("gold", "Sovereign Gold", "ذهبي ملكي", "#D4AF37"),
            PremiumColor("parchment", "Warm Parchment", "أوف وايت ناعم", "#F4F0E6"),
            PremiumColor("obsidian", "Obsidian Black", "أسود فحم", "#1A1A1A"),
            PremiumColor("purple", "Imperial Purple", "بنفسجي إمبريالي", "#6D214F"),
            PremiumColor("ochre", "Burnt Ochre", "أوكر برتقالي كلاسيك", "#D35400")
        )
    }

    var selectedMaterial by remember { mutableStateOf(premiumMaterialsList[0]) }
    var selectedColor by remember { mutableStateOf(premiumColorsList[0]) }

    // Synchronize selectedMaterial & selectedColor once result returns or changes to keep consistency
    LaunchedEffect(aiResult) {
        val res = aiResult
        if (res != null) {
            // Find best matching color from preset color list
            val colorMatch = premiumColorsList.find { it.hexValue.equals(res.primaryColorHex, ignoreCase = true) }
            if (colorMatch != null) {
                selectedColor = colorMatch
            }
            // Find best matching fabric
            val fabricMatch = premiumMaterialsList.find { mat ->
                res.suggestedFabrics.any { sug -> mat.nameEn.contains(sug, ignoreCase = true) || sug.contains(mat.nameEn, ignoreCase = true) }
            }
            if (fabricMatch != null) {
                selectedMaterial = fabricMatch
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Casino, contentDescription = "AI", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("AI Design Studio", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }
        Text(
            if (userLanguage == "ar") "اكتب فكرتك بالكامل لملابسك المخصصة وسيقوم الذكاء الاصطناعي برسمها وتفصيل مقاساتها فوراً" 
            else "Input text descriptions of garment ideas and let the Gemini API analyze & generate initial custom design sketches.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        // Mode Switching Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf(
                Pair(if (userLanguage == "ar") "تصميم برسمة بالوصف" else "Custom Idea Sketch", 0),
                Pair(if (userLanguage == "ar") "مساعد المناسبات الموجه" else "Guided Assistant", 1)
            )
            tabs.forEach { (title, idx) ->
                val isSelected = studioModeTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { studioModeTab = idx }
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .padding(vertical = 10.dp)
                        .testTag("studio_mode_tab_$idx"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Divider()

        if (aiResult == null) {
            if (studioModeTab == 0) {
                // CASE 1: Custom Freeform Prompt Text
                Text(
                    text = if (userLanguage == "ar") "صِف فكرتك لقطعة الملابس:" else "Describe your custom garment idea:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = garmentPrompt,
                    onValueChange = { garmentPrompt = it },
                    placeholder = {
                        Text(
                            if (userLanguage == "ar")
                                "مثال: عباءة أو فستان بلون أخضر زمردي مع ياقة عالية مرتفعة وتطريز ذهبي خفيف"
                            else
                                "e.g., An emerald green modern dress with a high collar, gold pocket button lines, and short sleeves"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_garment_prompt_input"),
                    minLines = 3,
                    maxLines = 5
                )

                // ----------------------------------------------------
                // PRE-GENERATION MATERIAL & COLOR SELECT PORTAL
                // ----------------------------------------------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (userLanguage == "ar") "🛠️ مواصفات القماش والتلوين المبدئية:" else "🛠️ Initial Material & Hue Settings:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        MaterialSelectionComponent(
                            userLanguage = userLanguage,
                            selectedMaterial = selectedMaterial,
                            onMaterialSelected = { selectedMaterial = it },
                            selectedColor = selectedColor,
                            onColorSelected = { selectedColor = it }
                        )
                    }
                }

                Text(
                    text = if (userLanguage == "ar") "أفكار ملهمة (اضغط للتجربة):" else "Inspirations (Tap to try):",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                val inspirations = listOf(
                    Pair(
                        if (userLanguage == "ar") "🟢 قفطان أخضر زمردي مطرز بالذهب" else "🟢 Emerald Green Kaftan & gold button",
                        "Emerald green light linen kaftan with high-collar mandarin neck, long sleeves, and gold embroidered stitch decorations"
                    ),
                    Pair(
                        if (userLanguage == "ar") "🔵 بدلة رسمية كاجوال باللون الكحلي" else "🔵 Indigo Tailored Casual Suit",
                        "Midnight indigo blue soft cashmere jacket suit with blazer lapels, long sleeves, and double parallel black buttons"
                    ),
                    Pair(
                        if (userLanguage == "ar") "🔴 فستان أحمر جريء بدون أكمام" else "🔴 Bold Red Sleeveless Dress",
                        "Crimson red formal flowing silk dress, sleeveless style with classic round neck and custom waist belt lines"
                    ),
                    Pair(
                        if (userLanguage == "ar") "⚪ قميص كتان مريح باللون الأبيض" else "⚪ Classic White Linen V-Neck Shirt",
                        "Off-white relaxed fit linen shirt with comfortable v-neck collar, short sleeves, and single pocket detail"
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    inspirations.forEach { (label, fullPrompt) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { garmentPrompt = fullPrompt }
                                .testTag("inspire_${label.split(" ").firstOrNull() ?: "test"}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFF1C40F), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (aiLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (userLanguage == "ar") "جاري الاتصال بـ Gemini لرسم الفكرة وتوليد الأبعاد..." else "Gemini is designing your custom outfit blueprint...",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (garmentPrompt.isNotBlank()) {
                                // Enrich user description with exact selected material & color values so Gemini understands intent perfectly!
                                val enrichedPrompt = if (userLanguage == "ar") {
                                    "$garmentPrompt (تصميم مخصص بقماش ${selectedMaterial.nameEn} بلون أساسي ${selectedColor.nameEn} كود ${selectedColor.hexValue})"
                                } else {
                                    "$garmentPrompt (Tailored using fabric: ${selectedMaterial.nameEn} and prominent hex color: ${selectedColor.hexValue})"
                                }
                                appViewModel.triggerAIDesignSketch(enrichedPrompt)
                            }
                        },
                        enabled = garmentPrompt.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("ai_generate_sketch_button")
                    ) {
                        Text(if (userLanguage == "ar") "توليد رسمة ومواصفات التصميم" else "Draft Sketch & Design specs")
                    }
                }
            } else {
                // CASE 2: Guided Occasion Selections
                Text("Occasion / Purpose of Outfit", fontWeight = FontWeight.Bold)
                val occasions = listOf("Formal wedding", "Corporate business", "Casual Beach wear", "Modern Street wear")
                occasions.forEach { occ ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { occasion = occ }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = occasion == occ, onClick = { occasion = occ })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(occ)
                    }
                }

                Text("Aesthetic Style Selection", fontWeight = FontWeight.Bold)
                val styles = listOf("Elegant", "Minimalist", "Bold & Creative", "Heritage Traditional")
                styles.forEach { st ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { style = st }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = style == st, onClick = { style = st })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(st)
                    }
                }

                // ----------------------------------------------------
                // CASE 2 MATERIAL & COLOR CONFIG PANEL
                // ----------------------------------------------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        MaterialSelectionComponent(
                            userLanguage = userLanguage,
                            selectedMaterial = selectedMaterial,
                            onMaterialSelected = { selectedMaterial = it },
                            selectedColor = selectedColor,
                            onColorSelected = { selectedColor = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = bodyType,
                    onValueChange = { bodyType = it },
                    label = { Text("Describe frame details (e.g., Slim, Athletic, Broad Shoulder)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = budgetStr,
                    onValueChange = { budgetStr = it },
                    label = { Text("Your Estimated Budget (EGP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (aiLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("AI Consulting Egyptian Tailor Masters...", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Button(
                        onClick = {
                            val budget = budgetStr.toDoubleOrNull() ?: 2000.0
                            appViewModel.triggerAIDesign(
                                occasion = occasion,
                                style = style,
                                bodyType = bodyType,
                                fabric = selectedMaterial.nameEn,
                                budget = budget,
                                colors = listOf(selectedColor.nameEn)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("ai_generate_button")
                    ) {
                        Text("Generate Custom AI Design Idea")
                    }
                }
            }
        } else {
            // Displays generated design specs and active technical drawing sketch
            val res = aiResult!!

            // Here we derive the customized outcome based on real-time material selection browsing on the client after generation!
            val customizedResult = remember(res, selectedMaterial, selectedColor) {
                // Base cost = Basic assembly master tailor rate (1500 EGP) + 3 meters of our selected premium luxury fabric!
                val tailoredPrice = 1500.0 + (3.0 * selectedMaterial.priceEgp)
                res.copy(
                    primaryColorHex = selectedColor.hexValue,
                    suggestedFabrics = listOf(selectedMaterial.nameEn),
                    estimatedPrice = tailoredPrice
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = customizedResult.title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (customizedResult.isApiPowered) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.secondary
                        ) {
                            Text(
                                text = if (customizedResult.isApiPowered) "Gemini Generated" else "AI Craft Pro",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = customizedResult.description, style = MaterialTheme.typography.bodyMedium)

                    // RENDER THE ACTIVE TAILOR SKETCH BLUEPRINT NATIVE CANVAS (Dynamically updates with user's browsed/selected colorhex!)
                    if (customizedResult.garmentType != null || studioModeTab == 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (userLanguage == "ar") "رسمة الموديل المبدئية (Initial Design Sketch)" else "Initial Tailoring Blueprint Sketch:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Drawn custom model instantly updates live when they try different shades on color scroll!
                        BespokeSketchCanvas(result = customizedResult)
                    }

                    // ----------------------------------------------------
                    // POST-GENERATION LIVE MATERIAL/COLOR SWAP BROWSER
                    // ----------------------------------------------------
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (userLanguage == "ar") "تعديل واختيار نوع الخامة واللون للرسمة:" else "Personalize Blueprint Fabric & Hue Selection:",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = if (userLanguage == "ar") "تصفح الخامات أدناه لتعديل مظهر الموديل الجمالي وتحديث تكلفة التفصيل الإجمالية مباشرة."
                               else "Browse our premier mills catalog below to re-color the schematic canvas and instantly recalculate prices.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    MaterialSelectionComponent(
                        userLanguage = userLanguage,
                        selectedMaterial = selectedMaterial,
                        onMaterialSelected = { selectedMaterial = it },
                        selectedColor = selectedColor,
                        onColorSelected = { selectedColor = it }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Recommended Fabrics Detail:", fontWeight = FontWeight.Bold)
                    Text(customizedResult.suggestedFabrics.joinToString(", ") + " (${if (userLanguage == "ar") "قماش نخب أول" else "Custom Selected Type"})")

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Master Crafting Directives:", fontWeight = FontWeight.Bold)
                    customizedResult.tailoringSecrets.forEach { spec ->
                        Text("• $spec", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (userLanguage == "ar") "حساب التكلفة الإجمالية والوزن المخصص:" else "Interactive Tailoring Estimate Breakdown:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• " + if (userLanguage == "ar") "الخامة المحددة: ${selectedMaterial.nameEn} (${selectedMaterial.priceEgp} ج.م للمتر) = ${selectedMaterial.priceEgp * 3} ج.م"
                                       else "Custom Swatch: ${selectedMaterial.nameEn} (${selectedMaterial.priceEgp} EGP/m) = ${selectedMaterial.priceEgp * 3} EGP",
                                fontSize = 11.sp
                            )
                            Text(
                                text = "• " + if (userLanguage == "ar") "تكاليف أتعاب حياكة الأستاذ ماركو: ١٥٠٠ ج.م" else "Sartorial Tailoring Base Rate: 1500 EGP",
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Price Suggestion: ${customizedResult.estimatedPrice.toInt()} EGP",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Detailed Stitching Method / Steps:", fontWeight = FontWeight.Bold)
                    customizedResult.assemblySteps.forEach { step ->
                        Text("• $step", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                OutlinedButton(
                    onClick = { appViewModel.clearAIGeneratorResult() },
                    modifier = Modifier.weight(1f).testTag("ai_clear_design_button")
                ) {
                    Text("Clear Design")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        // Place order based on AI Result and user's chosen custom material & color
                        appViewModel.placeCustomOrder(
                            category = "AI Creation",
                            templateName = customizedResult.title,
                            colorHex = selectedColor.hexValue.removePrefix("#"),
                            fabricName = selectedMaterial.nameEn,
                            notes = "AI Specifications: ${customizedResult.description}. Fabric: ${selectedMaterial.nameEn} @ ${selectedMaterial.threadInfoEn}. Steps: ${customizedResult.assemblySteps.joinToString()}",
                            sizeType = "M",
                            length = 72f,
                            width = 52f,
                            sleeve = 62f,
                            collar = 40f,
                            deliveryAddress = "5 El Maadi St, Cairo",
                            deliveryNotes = "AI Design Direct Order - Custom selected Material",
                            paymentMethod = "COD",
                            price = customizedResult.estimatedPrice
                        )
                        onSubmittedToTailor()
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("ai_submit_order")
                ) {
                    Text("Stitch with Marco Rossi")
                }
            }
        }
    }
}

// ----------------------------------------------------
// VIRTUAL TRY-ON (INTERACTIVE CANVAS LAYOVER)
// ----------------------------------------------------
@Composable
fun VirtualTryOn(appViewModel: AppViewModel) {
    var tryOnTargetMode by remember { mutableStateOf("MANNEQUIN") } // "MANNEQUIN", "CAMERA", "AVATAR"
    var isCameraCaptured by remember { mutableStateOf(false) }
    var selectedAvatarIndex by remember { mutableStateOf(0) } // 0 = Zain, 1 = Farida, 2 = Youssef
    var isCameraFlashing by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableStateOf(-1) } // countdown, -1 is idle
    var hasTimer by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var genderIndex by remember { mutableStateOf("Male") } // "Male" or "Female" or "Curvy"
    var selectedGarmentCategory by remember { mutableStateOf("Shirt") } // "Shirt", "Dress", "Suit", "Jacket"
    var colorOverlayHex by remember { mutableStateOf("E67E22") } // Orange initial
    var overlayOpacity by remember { mutableStateOf(0.85f) }

    fun triggerShutter() {
        coroutineScope.launch {
            if (hasTimer) {
                countdownValue = 3
                while (countdownValue > 0) {
                    delay(1000)
                    countdownValue--
                }
            }
            isCameraFlashing = true
            delay(150)
            isCameraFlashing = false
            isCameraCaptured = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Face, contentDescription = "TryOn", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Virtual Try-On Sandbox", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }

        Text(
            "تجربة الملابس الافتراضية على مانيكان تفاعلي مع كاميرا تجريبية أو اختيار موديل خاص",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Divider()

        // Mode select buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val modes = listOf(
                "MANNEQUIN" to "Standard Silhouette",
                "CAMERA" to "Camera Sandbox",
                "AVATAR" to "Fashion Models"
            )
            modes.forEach { (mKey, mTitle) ->
                val isSel = tryOnTargetMode == mKey
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            tryOnTargetMode = mKey
                            if (mKey == "AVATAR") {
                                genderIndex = when (selectedAvatarIndex) {
                                    0 -> "Male"
                                    1 -> "Female"
                                    else -> "Curvy"
                                }
                            }
                        }
                        .testTag("mode_$mKey"),
                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = mTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            // Left pane: Interactive Visual Render on Canvas
            Card(
                modifier = Modifier
                    .weight(1.2f)
                    .height(320.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val colorAccent = Color(android.graphics.Color.parseColor("#$colorOverlayHex"))

                    if (tryOnTargetMode == "CAMERA" && !isCameraCaptured) {
                        // Simulated scanning live camera preview background
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1E212D))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                // Draw futuristic alignment guides
                                val guideColor = Color(0x6600FF99)
                                repeat(4) { i ->
                                    val y = h * (i + 1) / 5f
                                    drawLine(color = guideColor, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(w, y), strokeWidth = 1.dp.toPx())
                                    val x = w * (i + 1) / 5f
                                    drawLine(color = guideColor, start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x, h), strokeWidth = 1.dp.toPx())
                                }

                                // Neon target contour lines
                                drawCircle(
                                    color = Color(0xFF00FF99),
                                    radius = 24.dp.toPx(),
                                    center = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.16f),
                                    style = Stroke(width = 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
                                )

                                val guideTorso = Path().apply {
                                    moveTo(w / 2f - 38.dp.toPx(), h * 0.28f)
                                    lineTo(w / 2f + 38.dp.toPx(), h * 0.28f)
                                    lineTo(w / 2f + 30.dp.toPx(), h * 0.58f)
                                    lineTo(w / 2f - 30.dp.toPx(), h * 0.58f)
                                    close()
                                }
                                drawPath(path = guideTorso, color = Color(0xFF00FF99).copy(alpha = 0.5f), style = Stroke(width = 2.dp.toPx()))
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (countdownValue > 0) {
                                    Text(
                                        text = "$countdownValue",
                                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
                                        color = Color(0xFF00FF99)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Taking snap...", color = Color.White, fontSize = 11.sp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = "Lens Active",
                                        tint = Color(0xFF00FF99),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("LIVE CAMERA VIEW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FF99), textAlign = TextAlign.Center)
                                    Text("Align with standard contour", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
                                }
                            }

                            // Simulation Active pill on Top Left
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(Color.White, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("REC", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Drawing base Canvas: Mannequin, Avatar, or Captured Camera snapshot
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (tryOnTargetMode == "CAMERA") {
                                        Brush.verticalGradient(listOf(Color(0xFF242B35), Color(0xFF13171D)))
                                    } else {
                                        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant))
                                    }
                                )
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                // 1. Determine body skin/silhouette color
                                val bodyColor = when (tryOnTargetMode) {
                                    "AVATAR" -> when (selectedAvatarIndex) {
                                        0 -> Color(0xFFE6C3A7) // Zain warm skin
                                        1 -> Color(0xFFD3A992) // Farida olive skin
                                        else -> Color(0xFFC49A76) // Youssef rich gold skin
                                    }
                                    "CAMERA" -> Color(0xFF76E1CA).copy(alpha = 0.5f) // Glowing cyan matrix
                                    else -> Color(0xFFE5DDD5) // default Standard base
                                }

                                // 2. Draw base body structure
                                // Head
                                drawCircle(
                                    color = bodyColor,
                                    radius = if (tryOnTargetMode == "CAMERA") 24.dp.toPx() else 21.dp.toPx(),
                                    center = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.16f)
                                )
                                // Neck
                                drawRect(
                                    color = bodyColor,
                                    size = androidx.compose.ui.geometry.Size(12.dp.toPx(), 20.dp.toPx()),
                                    topLeft = androidx.compose.ui.geometry.Offset(w / 2f - 6.dp.toPx(), h * 0.16f + 16.dp.toPx())
                                )

                                // Torso path depending on frame index
                                val torsoPath = Path().apply {
                                    moveTo(w / 2f - 40.dp.toPx(), h * 0.25f)
                                    lineTo(w / 2f + 40.dp.toPx(), h * 0.25f)
                                    if (genderIndex == "Curvy") {
                                        lineTo(w / 2f + 48.dp.toPx(), h * 0.38f)
                                        lineTo(w / 2f + 30.dp.toPx(), h * 0.44f)
                                        lineTo(w / 2f + 42.dp.toPx(), h * 0.58f)
                                        lineTo(w / 2f - 42.dp.toPx(), h * 0.58f)
                                        lineTo(w / 2f - 30.dp.toPx(), h * 0.44f)
                                        lineTo(w / 2f - 48.dp.toPx(), h * 0.38f)
                                    } else {
                                        lineTo(w / 2f + 32.dp.toPx(), h * 0.45f)
                                        lineTo(w / 2f + 36.dp.toPx(), h * 0.58f)
                                        lineTo(w / 2f - 36.dp.toPx(), h * 0.58f)
                                        lineTo(w / 2f - 32.dp.toPx(), h * 0.45f)
                                    }
                                    close()
                                }
                                drawPath(path = torsoPath, color = bodyColor)

                                // Arms outline
                                val armPathLeft = Path().apply {
                                    moveTo(w / 2f - 40.dp.toPx(), h * 0.25f)
                                    lineTo(w / 2f - 52.dp.toPx(), h * 0.45f)
                                    lineTo(w / 2f - 50.dp.toPx(), h * 0.55f)
                                }
                                drawPath(path = armPathLeft, color = bodyColor, style = Stroke(width = 11.dp.toPx()))

                                val armPathRight = Path().apply {
                                    moveTo(w / 2f + 40.dp.toPx(), h * 0.25f)
                                    lineTo(w / 2f + 52.dp.toPx(), h * 0.45f)
                                    lineTo(w / 2f + 50.dp.toPx(), h * 0.55f)
                                }
                                drawPath(path = armPathRight, color = bodyColor, style = Stroke(width = 11.dp.toPx()))

                                // Legs outline
                                drawRect(
                                    color = bodyColor,
                                    size = androidx.compose.ui.geometry.Size(14.dp.toPx(), h * 0.35f),
                                    topLeft = androidx.compose.ui.geometry.Offset(w / 2f - 26.dp.toPx(), h * 0.58f)
                                )
                                drawRect(
                                    color = bodyColor,
                                    size = androidx.compose.ui.geometry.Size(14.dp.toPx(), h * 0.35f),
                                    topLeft = androidx.compose.ui.geometry.Offset(w / 2f + 12.dp.toPx(), h * 0.58f)
                                )

                                // 3. Draw Character/Avatar Details (Distinctive head line art/accessories)
                                if (tryOnTargetMode == "AVATAR") {
                                    when (selectedAvatarIndex) {
                                        0 -> { // Zain
                                            // Black trendy sleek hair top
                                            drawArc(
                                                color = Color(0xFF1E272C),
                                                startAngle = 180f,
                                                sweepAngle = 180f,
                                                useCenter = true,
                                                size = androidx.compose.ui.geometry.Size(42.dp.toPx(), 20.dp.toPx()),
                                                topLeft = androidx.compose.ui.geometry.Offset(w / 2f - 21.dp.toPx(), h * 0.16f - 22.dp.toPx())
                                            )
                                            // Sleek glasses
                                            drawRect(
                                                color = Color(0xFF2C3E50),
                                                size = androidx.compose.ui.geometry.Size(30.dp.toPx(), 7.dp.toPx()),
                                                topLeft = androidx.compose.ui.geometry.Offset(w / 2f - 15.dp.toPx(), h * 0.16f - 3.dp.toPx())
                                            )
                                        }
                                        1 -> { // Farida
                                            // Flowing hair base
                                            drawCircle(
                                                color = Color(0xFF4A2F1D),
                                                radius = 23.dp.toPx(),
                                                center = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.16f - 2.dp.toPx())
                                            )
                                            // Purple hair scarf / headband
                                            drawCircle(
                                                color = Color(0xFF8E44AD),
                                                radius = 15.dp.toPx(),
                                                center = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.16f + 16.dp.toPx()),
                                                style = Stroke(width = 4.dp.toPx())
                                            )
                                        }
                                        else -> { // Youssef
                                            // Red sports cap style
                                            val sportsCap = Path().apply {
                                                moveTo(w / 2f - 20.dp.toPx(), h * 0.16f - 11.dp.toPx())
                                                lineTo(w / 2f + 20.dp.toPx(), h * 0.16f - 11.dp.toPx())
                                                lineTo(w / 2f + 30.dp.toPx(), h * 0.16f - 20.dp.toPx())
                                                lineTo(w / 2f - 10.dp.toPx(), h * 0.16f - 20.dp.toPx())
                                                close()
                                            }
                                            drawPath(path = sportsCap, color = Color(0xFFC0392B))
                                        }
                                    }
                                } else if (tryOnTargetMode == "CAMERA") {
                                    // Simulated digitized telemetry brackets around face
                                    drawCircle(
                                        color = Color(0xFF00FF99),
                                        radius = 29.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.16f),
                                        style = Stroke(width = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                                    )
                                }

                                // 4. Overlay chosen garment
                                val clothColor = colorAccent.copy(alpha = overlayOpacity)
                                when (selectedGarmentCategory) {
                                    "Shirt" -> {
                                        val shirtPath = Path().apply {
                                            moveTo(w / 2f - 42.dp.toPx(), h * 0.24f)
                                            lineTo(w / 2f + 42.dp.toPx(), h * 0.24f)
                                            lineTo(w / 2f + 36.dp.toPx(), h * 0.48f)
                                            lineTo(w / 2f - 36.dp.toPx(), h * 0.48f)
                                            close()
                                        }
                                        drawPath(path = shirtPath, color = clothColor)

                                        val armOverLeft = Path().apply {
                                            moveTo(w / 2f - 40.dp.toPx(), h * 0.25f)
                                            lineTo(w / 2f - 46.dp.toPx(), h * 0.34f)
                                        }
                                        drawPath(path = armOverLeft, color = clothColor, style = Stroke(width = 14.dp.toPx()))
                                        val armOverRight = Path().apply {
                                            moveTo(w / 2f + 40.dp.toPx(), h * 0.25f)
                                            lineTo(w / 2f + 46.dp.toPx(), h * 0.34f)
                                        }
                                        drawPath(path = armOverRight, color = clothColor, style = Stroke(width = 14.dp.toPx()))
                                    }
                                    "Dress" -> {
                                        val dressPath = Path().apply {
                                            moveTo(w / 2f - 32.dp.toPx(), h * 0.25f)
                                            lineTo(w / 2f + 32.dp.toPx(), h * 0.25f)
                                            lineTo(w / 2f + 55.dp.toPx(), h * 0.85f)
                                            lineTo(w / 2f - 55.dp.toPx(), h * 0.85f)
                                            close()
                                        }
                                        drawPath(path = dressPath, color = clothColor)
                                    }
                                    "Suit" -> {
                                        val jacketPath = Path().apply {
                                            moveTo(w / 2f - 44.dp.toPx(), h * 0.24f)
                                            lineTo(w / 2f + 44.dp.toPx(), h * 0.24f)
                                            lineTo(w / 2f + 38.dp.toPx(), h * 0.54f)
                                            lineTo(w / 2f - 38.dp.toPx(), h * 0.54f)
                                            close()
                                        }
                                        drawPath(path = jacketPath, color = clothColor)

                                        val pantsPath = Path().apply {
                                            moveTo(w / 2f - 28.dp.toPx(), h * 0.54f)
                                            lineTo(w / 2f + 28.dp.toPx(), h * 0.54f)
                                            lineTo(w / 2f + 24.dp.toPx(), h * 0.88f)
                                            lineTo(w / 2f + 10.dp.toPx(), h * 0.88f)
                                            lineTo(w / 2f, h * 0.58f)
                                            lineTo(w / 2f - 10.dp.toPx(), h * 0.88f)
                                            lineTo(w / 2f - 24.dp.toPx(), h * 0.88f)
                                            close()
                                        }
                                        drawPath(path = pantsPath, color = clothColor)
                                    }
                                    "Jacket" -> {
                                        val jacketPath = Path().apply {
                                            moveTo(w / 2f - 46.dp.toPx(), h * 0.23f)
                                            lineTo(w / 2f + 46.dp.toPx(), h * 0.23f)
                                            lineTo(w / 2f + 40.dp.toPx(), h * 0.62f)
                                            lineTo(w / 2f - 40.dp.toPx(), h * 0.62f)
                                            close()
                                        }
                                        drawPath(path = jacketPath, color = clothColor)
                                    }
                                }
                            }

                            // Subtitle Lock Overlay info banner
                            if (tryOnTargetMode == "CAMERA" && isCameraCaptured) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.82f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Check, contentDescription = "Active", tint = Color(0xFF00FF99), modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Sizing Snapshot Locked", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text = "RETAKE",
                                            modifier = Modifier.clickable { isCameraCaptured = false }.testTag("camera_retake"),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Camera shutter flash animation frame
                    if (isCameraFlashing) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.White))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right Pane: Context-aware parameters controls (Standard, Camera snapshots details, or Avatar models selector)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (tryOnTargetMode == "MANNEQUIN") {
                    Text("Body Structure", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    val structureTypes = listOf("Male", "Female", "Curvy")
                    structureTypes.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { genderIndex = s }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = genderIndex == s, onClick = { genderIndex = s })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(s, fontSize = 12.sp)
                        }
                    }
                } else if (tryOnTargetMode == "AVATAR") {
                    Text("Choose Model Profile", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    val avatarsList = listOf(
                        Triple("Zain", "Male", "Modern Egypt Chic"),
                        Triple("Farida", "Female", "Alexandria Glamour"),
                        Triple("Youssef", "Curvy", "Heritage Fit")
                    )
                    avatarsList.forEachIndexed { index, (name, gender, sub) ->
                        val isSelected = selectedAvatarIndex == index
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAvatarIndex = index
                                    genderIndex = gender
                                }
                                .testTag("btn_avatar_$index"),
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("$gender • $sub", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                } else {
                    // CAMERA Sandbox settings
                    Text("Camera Calibration", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Simulate realistic camera device framing for try-on.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = hasTimer, onClick = { hasTimer = !hasTimer })
                                Text("3s Delay Timer", fontSize = 11.sp, modifier = Modifier.clickable { hasTimer = !hasTimer })
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = isFlashOn, onClick = { isFlashOn = !isFlashOn })
                                Text("Camera Flash Bulb", fontSize = 11.sp, modifier = Modifier.clickable { isFlashOn = !isFlashOn })
                            }
                        }
                    }

                    if (!isCameraCaptured) {
                        Button(
                            onClick = { triggerShutter() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("btn_shutter_capture"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Capture", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("Snapshot Ready!", color = Color(0xFF2ECC71), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text("Clothing Template", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                val itemsList = listOf("Shirt" to "قميص", "Dress" to "فستان", "Suit" to "بدلة", "Jacket" to "جاكيت")
                itemsList.forEach { (key, arabic) ->
                    val selected = selectedGarmentCategory == key
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { selectedGarmentCategory = key }
                            .testTag("try_$key"),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "$key ($arabic)", fontSize = 11.sp, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // Color Swatches
        Text("Colors Try-Out Swatches Selector", fontWeight = FontWeight.Bold)
        val swatchesColors = listOf(
            "FFFFFF" to "Cream",
            "2C3E50" to "Navy",
            "E67E22" to "Orange",
            "9E2A2B" to "Crimson",
            "196F3D" to "Emerald",
            "1F618D" to "Sapphire",
            "5B2C6F" to "Royal Purple"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            swatchesColors.forEach { (hex, name) ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor("#$hex")))
                        .border(
                            border = if (colorOverlayHex == hex) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, Color.LightGray),
                            shape = CircleShape
                        )
                        .clickable { colorOverlayHex = hex }
                        .testTag("try_color_$hex")
                )
            }
        }

        Text("Opacity Density (كثافة الخامة): ${(overlayOpacity * 100).toInt()}%")
        Slider(value = overlayOpacity, onValueChange = { overlayOpacity = it }, valueRange = 0.4f..1.0f)
    }
}

// ----------------------------------------------------
// DESIGNER MARKETPLACE SCREEN
// ----------------------------------------------------
@Composable
fun DesignerMarketplace(
    appViewModel: AppViewModel,
    onCheckoutCompleted: () -> Unit
) {
    val designs by appViewModel.marketplaceDesignsList.collectAsState()
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    var showingAddListingDialog by remember { mutableStateOf(false) }
    var showingBuyDialog by remember { mutableStateOf<MarketplaceDesignEntity?>(null) }

    // Add listing states
    var listTitle by remember { mutableStateOf("") }
    var listDesc by remember { mutableStateOf("") }
    var listCategory by remember { mutableStateOf("Shirt") }
    var listPrice by remember { mutableStateOf("") }

    // Checkout Form state
    var checkoutAddress by remember { mutableStateOf("5 El Maadi St, Cairo") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, contentDescription = "Shop", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Designer Marketplace", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            }

            Button(
                onClick = { showingAddListingDialog = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("action_list_design")
            ) {
                Text("+ List Design")
            }
        }

        // Category Filters
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf("All", "Shirt", "Dress", "Suit", "Jacket")
            categories.forEach { cat ->
                val active = selectedCategoryFilter == cat
                FilterChip(
                    selected = active,
                    onClick = { selectedCategoryFilter = cat },
                    label = { Text(cat) }
                )
            }
        }

        val filteredDesigns = if (selectedCategoryFilter == "All") {
            designs
        } else {
            designs.filter { it.category == selectedCategoryFilter }
        }

        if (filteredDesigns.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No pieces available in this category.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredDesigns) { design ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Dummy visual color accent block to represent design
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(NavyPrimary, BlueAccent)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    design.title.take(2).uppercase(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                design.title,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "By ${design.designerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                design.description,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${design.price.toInt()} EGP",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (design.isSold) {
                                    Surface(color = Color.LightGray, shape = RoundedCornerShape(4.dp)) {
                                        Text("Sold out", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp)
                                    }
                                } else {
                                    var btnTag = "buy_design_${design.id}"
                                    Button(
                                        onClick = { showingBuyDialog = design },
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier
                                            .height(30.dp)
                                            .testTag(btnTag)
                                    ) {
                                        Text("Buy", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // LIST DESIGN FORM DIALOG
    if (showingAddListingDialog) {
        AlertDialog(
            onDismissRequest = { showingAddListingDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val price = listPrice.toDoubleOrNull() ?: 1000.0
                        appViewModel.createMarketplaceDesign(listTitle, listDesc, listCategory, price)
                        showingAddListingDialog = false
                    },
                    modifier = Modifier.testTag("submit_marketplace_listing")
                ) {
                    Text("Submit Piece")
                }
            },
            dismissButton = { TextButton(onClick = { showingAddListingDialog = false }) { Text("Cancel") } },
            title = { Text("Showcase design in Marketplace") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = listTitle, onValueChange = { listTitle = it }, label = { Text("Outfit Title (e.g. Silk Autumn Vest)") })
                    OutlinedTextField(value = listDesc, onValueChange = { listDesc = it }, label = { Text("Describe the look and stitching details...") })
                    OutlinedTextField(value = listPrice, onValueChange = { listPrice = it }, label = { Text("Price (EGP)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                    Text("Peculiar Category", fontWeight = FontWeight.Bold)
                    val categories = listOf("Shirt", "Dress", "Suit", "Jacket")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        categories.forEach { c ->
                            FilterChip(selected = listCategory == c, onClick = { listCategory = c }, label = { Text(c) })
                        }
                    }
                }
            }
        )
    }

    // BUY DIALOG (SANDBOX CHECKOUT INTEGRATED)
    if (showingBuyDialog != null) {
        val d = showingBuyDialog!!
        AlertDialog(
            onDismissRequest = { showingBuyDialog = null },
            confirmButton = {
                Button(
                    onClick = {
                        appViewModel.purchaseMarketplaceSelection(d.id, checkoutAddress)
                        showingBuyDialog = null
                        onCheckoutCompleted()
                    },
                    modifier = Modifier.testTag("confirm_marketplace_purchase")
                ) {
                    Text("Confirm Checkout (${d.price.toInt()} EGP)")
                }
            },
            dismissButton = { TextButton(onClick = { showingBuyDialog = null }) { Text("Cancel") } },
            title = { Text("Checkout Design: ${d.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Designer piece will be crafted custom by tailor Marco Rossi in 5 El Ghoureya.")
                    OutlinedTextField(value = checkoutAddress, onValueChange = { checkoutAddress = it }, label = { Text("Your Shipping Address") })
                    Text("Billing: Credit Card (Card saved from Sizing profile default)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

// ----------------------------------------------------
// INTERACTIVE WORKSHOP & SUPPLIER MAP
// ----------------------------------------------------
@Composable
fun InteractiveWorkshopMap(appViewModel: AppViewModel) {
    // Custom stylized Egyptian Landmark interactive card
    var selectedPinId by remember { mutableStateOf(1) } // Default Cairo workshop

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = "Map", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Egyptian Tailors Network Map", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }

        Text(
            "خريطة تفاعلية للورش المحلية ومواقع الموردين في مصر للربط اللوجستي السريع",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Divider()

        // Styled Custom Interactive Map Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box {
                // Background River Nile & Egyptian coast stylized drawing
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Styled Nile River path
                    val nile = Path().apply {
                        moveTo(w * 0.48f, h)
                        quadraticTo(w * 0.52f, h * 0.6f, w * 0.5f, h * 0.35f)
                        quadraticTo(w * 0.45f, h * 0.25f, w * 0.42f, 0f)
                    }
                    drawPath(path = nile, color = Color(0xFF3498DB).copy(alpha = 0.4f), style = Stroke(width = 18.dp.toPx()))

                    // Delta branching
                    val branchAlexFraction = Path().apply {
                        moveTo(w * 0.5f, h * 0.35f)
                        quadraticTo(w * 0.35f, h * 0.2f, w * 0.22f, 0f)
                    }
                    drawPath(path = branchAlexFraction, color = Color(0xFF3498DB).copy(alpha = 0.4f), style = Stroke(width = 12.dp.toPx()))
                }

                // Interactive Buttons overlay representing Cairo, Mahalla, and Alexandria Ports
                // 1. Marco Rossi Workshop (Ghoureya, Cairo)
                MapMarkerPin(
                    label = "Ghoureya Cairo (Workshop)",
                    xPercent = 0.51f,
                    yPercent = 0.44f,
                    selected = selectedPinId == 1,
                    onSelect = { selectedPinId = 1 },
                    modifier = Modifier.testTag("pin_cairo")
                )

                // 2. Priya Silk Supplier (Mahalla Industrial)
                MapMarkerPin(
                    label = "Mahalla (Supplier)",
                    xPercent = 0.48f,
                    yPercent = 0.22f,
                    selected = selectedPinId == 2,
                    onSelect = { selectedPinId = 2 },
                    modifier = Modifier.testTag("pin_mahalla")
                )

                // 3. Karim Logistics Dispatch (Downtown Cairo)
                MapMarkerPin(
                    label = "Ramses Cairo (Logistics)",
                    xPercent = 0.56f,
                    yPercent = 0.38f,
                    selected = selectedPinId == 3,
                    onSelect = { selectedPinId = 3 },
                    modifier = Modifier.testTag("pin_ramses")
                )
            }
        }

        // Details of selected pin
        val selectedPinDetails = when (selectedPinId) {
            1 -> Triple("Marco Rossi - ورشة الغورية", "12 El Ghoureya, Cairo", "Craft Tailor Shop specialized in bespoke suits, linen shirts and silk evening dresses. Sells custom outfits to end clients with full courier support.")
            2 -> Triple("Priya Premium Textures - المحلة", "Industrial Zone, El Mahalla", "High-volume suppliers of premium long-staple Egyptian cotton, pure flax linen, and Mulberry silk. Handles express deliveries directly to craft workshops.")
            else -> Triple("Karim Delivery Logistics - رمسيس", "18 Ramses El Thany, Cairo", "Central local delivery network and dispatch hub, carrying custom patterns, materials, and finalised garments straight to end-user avenues.")
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(selectedPinDetails.first, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Text("Address: ${selectedPinDetails.second}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Text(selectedPinDetails.third, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun BoxScope.MapMarkerPin(
    label: String,
    xPercent: Float,
    yPercent: Float,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .align(Alignment.TopStart)
            .offset(
                x = (xPercent * 250).dp, // scaling coordinates visually
                y = (yPercent * 200).dp
            )
            .clickable { onSelect() }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = label,
                tint = if (selected) Color(0xFFE67E22) else Color(0xFF2C3E50),
                modifier = Modifier.size(if (selected) 36.dp else 26.dp)
            )
            if (selected) {
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// USER PROFILE SCREEN (MEASUREMENTS PROFILE INTEGRATED)
// ----------------------------------------------------
@Composable
fun UserProfileScreen(appViewModel: AppViewModel) {
    val currentUser by appViewModel.currentUser.collectAsState()
    val userLanguage by appViewModel.userLanguage.collectAsState()
    val themeMode by appViewModel.themeMode.collectAsState()

    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var showSuccessMsg by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        currentUser?.let {
            editName = it.name
            editPhone = it.phone
            editAddress = it.address
            editBio = it.bio
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = com.example.ui.Localization.get("my_account_profile", userLanguage),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Divider()

        // Theme and Language Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = com.example.ui.Localization.get("appearance", userLanguage),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = com.example.ui.Localization.get("settings_desc", userLanguage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                // Language Choices
                Text(
                    text = com.example.ui.Localization.get("language_setting", userLanguage),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val langs = listOf("en" to "English (English)", "ar" to "العربية (Arabic)")
                    langs.forEach { (code, label) ->
                        val isSel = userLanguage == code
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { appViewModel.setUserLanguage(code) },
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSel) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Theme Mode choices
                Text(
                    text = com.example.ui.Localization.get("appearance", userLanguage),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modes = listOf(
                        "system" to com.example.ui.Localization.get("system_theme", userLanguage),
                        "light" to com.example.ui.Localization.get("light_mode", userLanguage),
                        "dark" to com.example.ui.Localization.get("dark_mode", userLanguage)
                    )
                    modes.forEach { (mode, label) ->
                        val isSel = themeMode == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { appViewModel.setThemeMode(mode) },
                            color = if (isSel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSel) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Profile details Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val activeRoleLabel = when (currentUser?.role) {
                    "CUSTOMER" -> com.example.ui.Localization.get("customer_view", userLanguage)
                    "TAILOR" -> com.example.ui.Localization.get("tailor_view", userLanguage)
                    "SUPPLIER" -> com.example.ui.Localization.get("supplier_view", userLanguage)
                    "DELIVERY" -> com.example.ui.Localization.get("delivery_view", userLanguage)
                    else -> currentUser?.role ?: ""
                }

                Text(
                    text = "${com.example.ui.Localization.get("role_portal", userLanguage)}: $activeRoleLabel",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text(com.example.ui.Localization.get("display_name", userLanguage)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )

                OutlinedTextField(
                    value = editPhone,
                    onValueChange = { editPhone = it },
                    label = { Text(com.example.ui.Localization.get("phone_number", userLanguage)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editAddress,
                    onValueChange = { editAddress = it },
                    label = { Text(com.example.ui.Localization.get("address", userLanguage)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editBio,
                    onValueChange = { editBio = it },
                    label = { Text(com.example.ui.Localization.get("bio", userLanguage)) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (showSuccessMsg) {
                    Text(
                        text = com.example.ui.Localization.get("save_success", userLanguage),
                        color = Color(0xFF196F3D),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Button(
                    onClick = {
                        appViewModel.updateProfile(editName, editPhone, editAddress, editBio)
                        showSuccessMsg = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_save"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(com.example.ui.Localization.get("save_changes", userLanguage))
                }
            }
        }
    }
}

// ----------------------------------------------------
// SUPPLIER FABRIC INVENTORY & STOCK ESTIMATOR DASHBOARD
// ----------------------------------------------------
fun parseColorString(colorString: String): Color {
    val hexPattern = "#([A-Fa-f0-9]{6})".toRegex()
    val matchResult = hexPattern.find(colorString)
    if (matchResult != null) {
        return try {
            Color(android.graphics.Color.parseColor(matchResult.value))
        } catch (e: Exception) {
            Color(0xFF2C3E50)
        }
    }
    val lower = colorString.lowercase()
    return when {
        "white" in lower -> Color.White
        "navy" in lower -> Color(0xFF1B2A47)
        "blue" in lower || "cobalt" in lower || "indigo" in lower -> Color(0xFF1B4F72)
        "beige" in lower || "sand" in lower -> Color(0xFFE2C6A3)
        "crimson" in lower || "red" in lower || "burgundy" in lower -> Color(0xFF801515)
        "grey" in lower || "gray" in lower || "charcoal" in lower -> Color(0xFF4A4A4A)
        "green" in lower || "emerald" in lower -> Color(0xFF196F3D)
        "black" in lower || "obsidian" in lower || "coal" in lower -> Color(0xFF1C1C1C)
        "gold" in lower || "yellow" in lower -> Color(0xFFD4AF37)
        "purple" in lower || "plum" in lower -> Color(0xFF5B2C6F)
        "ochre" in lower || "orange" in lower || "rust" in lower -> Color(0xFFBA5A00)
        else -> Color(0xFF34495E)
    }
}

@Composable
fun FabricVisualSwatch(
    colorString: String,
    modifier: Modifier = Modifier
) {
    val fabricColor = remember(colorString) { parseColorString(colorString) }
    Box(
        modifier = modifier
            .background(fabricColor)
            .border(1.dp, Color.Gray.copy(alpha = 0.2f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthVal = 1.5.dp.toPx()
            val primaryWeaveColor = if (fabricColor == Color.White) {
                Color.LightGray.copy(alpha = 0.25f)
            } else {
                Color.White.copy(alpha = 0.12f)
            }
            val secondaryWeaveColor = if (fabricColor == Color.White) {
                Color.Gray.copy(alpha = 0.15f)
            } else {
                Color.Black.copy(alpha = 0.12f)
            }

            // Diagonal lines top-left to bottom-right
            for (offset in -400..1200 step 20) {
                drawLine(
                    color = primaryWeaveColor,
                    start = Offset(offset.toFloat(), 0f),
                    end = Offset((offset + size.height).toFloat(), size.height),
                    strokeWidth = strokeWidthVal
                )
            }

            // Cross-stitching lines: top-right to bottom-left
            for (offset in -400..1200 step 20) {
                drawLine(
                    color = secondaryWeaveColor,
                    start = Offset((size.width - offset).toFloat(), 0f),
                    end = Offset((size.width - offset - size.height).toFloat(), size.height),
                    strokeWidth = strokeWidthVal
                )
            }
        }
    }
}

fun getFabricRating(fabricId: Int, name: String): Double {
    return when(name) {
        "Egyptian Cotton Premium" -> 4.9
        "Italian Premium Wool" -> 4.8
        "Belgian Pure Linen" -> 4.7
        "Mulberry Silk" -> 4.9
        "Heavy Indigo Denim" -> 4.5
        else -> {
            val base = 4.5 + (fabricId % 5) * 0.1
            if (base > 5.0) 5.0 else base
        }
    }
}

fun getFabricReviewCount(fabricId: Int, name: String): Int {
    return when(name) {
        "Egyptian Cotton Premium" -> 142
        "Italian Premium Wool" -> 95
        "Belgian Pure Linen" -> 118
        "Mulberry Silk" -> 76
        "Heavy Indigo Denim" -> 64
        else -> 15 + (fabricId * 13) % 80
    }
}

fun getFabricGrade(name: String): String {
    return when(name) {
        "Egyptian Cotton Premium" -> "A+ Extra Long Staple"
        "Italian Premium Wool" -> "Grade A Super 140s"
        "Belgian Pure Linen" -> "Class-1 Certified Flax"
        "Mulberry Silk" -> "100% Organic Mulberry"
        "Heavy Indigo Denim" -> "Raw Japanese Selvage"
        else -> "Premium Commercial Grade"
    }
}

fun getFabricDeliveryDays(name: String): String {
    return when(name) {
        "Egyptian Cotton Premium" -> "1-2 Days (Cairo/Giza Express)"
        "Italian Premium Wool" -> "2-3 Days (Express Courier)"
        "Belgian Pure Linen" -> "2-4 Days (Standard Insured)"
        "Mulberry Silk" -> "1-2 Days (Next-day Priority)"
        "Heavy Indigo Denim" -> "3-5 Days (Standard Ground)"
        else -> "2-3 Business Days"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupplierInventoryDashboard(
    appViewModel: AppViewModel,
    onBack: () -> Unit
) {
    val fabrics by appViewModel.fabricsList.collectAsState()
    val userLanguage by appViewModel.userLanguage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedStockFilter by remember { mutableStateOf("All") } // "All", "In-Stock", "Low < 250m"
    var selectedSortOrder by remember { mutableStateOf("Default") } // "Default", "Price: Low to High", "Price: High to Low", "Ratings"

    var selectedFabricForDetail by remember { mutableStateOf<FabricEntity?>(null) }
    var inquiryMeters by remember { mutableStateOf(3f) }
    var inquiryNotes by remember { mutableStateOf("") }
    var showSuccessInquiryAlert by remember { mutableStateOf(false) }

    val filteredFabrics = remember(fabrics, searchQuery, selectedCategory, selectedStockFilter, selectedSortOrder) {
        var list = fabrics.filter { fab ->
            val matchesSearch = fab.name.contains(searchQuery, ignoreCase = true) || fab.color.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || fab.category.equals(selectedCategory, ignoreCase = true)
            val matchesStock = when (selectedStockFilter) {
                "In-Stock" -> fab.stockMs > 0
                "Low < 250m" -> fab.stockMs in 1..249
                else -> true
            }
            matchesSearch && matchesCategory && matchesStock
        }
        
        list = when (selectedSortOrder) {
            "Price: Low to High" -> list.sortedBy { it.pricePerMeter }
            "Price: High to Low" -> list.sortedByDescending { it.pricePerMeter }
            "Ratings" -> list.sortedByDescending { getFabricRating(it.id, it.name) }
            else -> list
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Master Title Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (userLanguage == "ar") "مخزون واستعلام الموردين" else "Supplier Fabrics Stock",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (userLanguage == "ar") "تتبع وتفحص خامات ومقاسات الأقمشة المعتمدة" else "Browse verified mills inventory & reserve custom swatches",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Search Bar Section
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (userLanguage == "ar") "ابحث عن قماش، لون، مواصفة هامة..." else "Search fabrics, hues, loom category...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("search_fabric"),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Category Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf("All", "Cotton", "Wool", "Linen", "Silk", "Denim")
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat) },
                    modifier = Modifier.testTag("chip_cat_$cat"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Extra Sort & Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sort chip toggle
            val sortOptions = listOf("Default", "Price: Low to High", "Price: High to Low", "Ratings")
            sortOptions.forEach { opt ->
                val isSelected = selectedSortOrder == opt
                SuggestionChip(
                    onClick = { selectedSortOrder = opt },
                    label = { Text(opt) },
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.testTag("sort_chip_$opt")
                )
            }

            Divider(modifier = Modifier.width(1.dp).height(24.dp))

            // Stock Toggle Chip
            val stockOptions = listOf("All", "In-Stock", "Low < 250m")
            stockOptions.forEach { stockOpt ->
                val isSelected = selectedStockFilter == stockOpt
                SuggestionChip(
                    onClick = { selectedStockFilter = stockOpt },
                    label = { Text(stockOpt) },
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fabric Listing Catalog
        if (filteredFabrics.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Layers, contentDescription = "Empty", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (userLanguage == "ar") "عذراً.. لم نجد أقمشة مطابقة لبحثك" else "No fabrics found in current category matching parameters",
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredFabrics) { fabric ->
                    val rating = getFabricRating(fabric.id, fabric.name)
                    val reviewCount = getFabricReviewCount(fabric.id, fabric.name)
                    val gradeText = getFabricGrade(fabric.name)
                    val deliveryText = getFabricDeliveryDays(fabric.name)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedFabricForDetail = fabric
                                inquiryMeters = 3f
                                inquiryNotes = ""
                                showSuccessInquiryAlert = false
                            }
                            .testTag("fabric_item_${fabric.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // High-Craft visual texture swatch
                            FabricVisualSwatch(
                                colorString = fabric.color,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column {
                                        Text(
                                            text = fabric.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${fabric.category} • $gradeText",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Ratings and Reviews Bar
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating Star",
                                        tint = Color(0xFFF1C40F),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$rating",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "($reviewCount reviews)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Stock Gauge Progress Indicator
                                val stockRatio = (fabric.stockMs / 500f).coerceIn(0f, 1f)
                                val stockColor = when {
                                    fabric.stockMs == 0 -> Color.Red
                                    fabric.stockMs < 250 -> Color(0xFFE67E22) // Orange
                                    else -> Color(0xFF27AE60) // Green
                                }
                                val stockLabel = when {
                                    fabric.stockMs == 0 -> if (userLanguage == "ar") "نفذ من المخزون ❌" else "Out of Stock ❌"
                                    fabric.stockMs < 250 -> if (userLanguage == "ar") "مخزون محدود (${fabric.stockMs}م) ⚠️" else "Low Stock (${fabric.stockMs}m) ⚠️"
                                    else -> if (userLanguage == "ar") "متوفر بكثرة (${fabric.stockMs}م) ✔" else "In Stock (${fabric.stockMs}m) ✔"
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stockLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = stockColor
                                        )
                                        Text(
                                            text = "Delivery: $deliveryText",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = stockRatio,
                                        color = stockColor,
                                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Pricing display
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${fabric.pricePerMeter.toInt()}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "EGP/m",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Fabric spec details dialog
    selectedFabricForDetail?.let { fabric ->
        val rating = getFabricRating(fabric.id, fabric.name)
        val reviewCount = getFabricReviewCount(fabric.id, fabric.name)
        val gradeText = getFabricGrade(fabric.name)
        val deliveryText = getFabricDeliveryDays(fabric.name)
        val calculatedCost = inquiryMeters * fabric.pricePerMeter
        val estimatedShipWeightGrams = inquiryMeters * when(fabric.category) {
            "Wool" -> 320
            "Denim" -> 280
            "Cotton" -> 160
            "Silk" -> 85
            else -> 190
        }

        AlertDialog(
            onDismissRequest = { selectedFabricForDetail = null },
            modifier = Modifier.testTag("fabric_dialog_${fabric.id}"),
            confirmButton = {
                Button(
                    onClick = {
                        appViewModel.createSampleSwatchInquiry(fabric.name, inquiryMeters.toDouble(), inquiryNotes)
                        showSuccessInquiryAlert = true
                    },
                    modifier = Modifier.testTag("request_swatch_button")
                ) {
                    Text(if (userLanguage == "ar") "أرسل طلب الحجز والعينة" else "Submit Swatch Inquiry")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedFabricForDetail = null }) {
                    Text(if (userLanguage == "ar") "إغلاق" else "Close")
                }
            },
            title = {
                Text(
                    text = fabric.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                if (showSuccessInquiryAlert) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF27AE60),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (userLanguage == "ar") "تم تقديم الطلب بنجاح!" else "Swatch Reservation Submitted!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF27AE60)
                        )
                        Text(
                            text = if (userLanguage == "ar") {
                                "تم حجز العينة بمقدار ${inquiryMeters}م وإرسال الطلب للمورد Priya Nair. يمكنك تتبع التقدم في قائمة التنبيهات."
                            } else {
                                "Successfully reserved swatch of ${inquiryMeters}m! A direct notification has been created. Priya Nair's mills department will process the dispatch."
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Wide Texture Swatch
                        FabricVisualSwatch(
                            colorString = fabric.color,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )

                        // Specification details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Category: ${fabric.category}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Loom Grade: $gradeText",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${fabric.pricePerMeter.toInt()} EGP/meter",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Stock: ${fabric.stockMs} Meters Left",
                                    fontSize = 11.sp,
                                    color = if (fabric.stockMs < 250) Color(0xFFE67E22) else Color(0xFF27AE60),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Divider()

                        // Description
                        Text(
                            text = fabric.description.ifBlank { "Verified luxury textile certified for premium sewing requirements." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        // Quality Ratings & Delivery Time Indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Sartorial Rating:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = "Star", tint = Color(0xFFF1C40F), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$rating", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(" ($reviewCount reviews)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Estimated Delivery:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = deliveryText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Divider()

                        // Interactive Fabric Calculator Section
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (userLanguage == "ar") "حاسبة أمتار التفصيل وتكلفة العينات:" else "Interactive Loom & Swatch Cost Calculator:",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (userLanguage == "ar") "المقدار المطلوب: ${"%.1f".format(inquiryMeters)} متر" else "Length: ${"%.1f".format(inquiryMeters)} Meters",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Estimated Weight: ${estimatedShipWeightGrams.toInt()}g",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Slider(
                                value = inquiryMeters,
                                onValueChange = { inquiryMeters = it },
                                valueRange = 1f..15f,
                                steps = 28,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Cost breakdown result card
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Base Fabric Price:", fontSize = 12.sp)
                                        Text(text = "${"%.1f".format(inquiryMeters)}m @ ${fabric.pricePerMeter.toInt()} EGP", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Total Price Quote:", fontWeight = FontWeight.Bold)
                                        Text(text = "${calculatedCost.toInt()} EGP", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        // Custom instructions note text box
                        OutlinedTextField(
                            value = inquiryNotes,
                            onValueChange = { inquiryNotes = it },
                            label = { Text(if (userLanguage == "ar") "ملاحظات تفصيلية لقص القماش..." else "Loom cut instructions / custom specs...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            placeholder = { Text(if (userLanguage == "ar") "مثال: أريد قطعة واحدة سليمة بطول المقدار كامل" else "e.g. Please supply a continuous uncut length") }
                        )
                    }
                }
            }
        )
    }
}

// ----------------------------------------------------
// ORDERS PORTAL (COVERS ALL PORTAL MANAGEMENT & CHAT SHORTCUTS)
// ----------------------------------------------------
@Composable
fun OrdersPortal(
    appViewModel: AppViewModel,
    onOpenChat: () -> Unit
) {
    val currentUser by appViewModel.currentUser.collectAsState()
    val allOrders by appViewModel.allOrdersList.collectAsState()

    // Filter list based on portals logged index
    val filteredOrders = when (currentUser?.role) {
        "CUSTOMER" -> allOrders.filter { it.customerId == currentUser?.id }
        "TAILOR" -> allOrders.filter { it.tailorId == currentUser?.id }
        "DELIVERY" -> allOrders.filter { it.deliveryPartnerId == currentUser?.id }
        else -> allOrders
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.List, contentDescription = "List", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("My Portal Orders (${filteredOrders.size})", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(13.dp))

        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No orders recorded on this account yet. Switch roles or design an outfit!")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredOrders) { order ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    order.orderNumber,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Surface(
                                    color = getStatusColor(order.status),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = order.status,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "${order.category} - ${order.templateName}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Material: ${order.fabricName} | Sizing: ${order.sizeType}", fontSize = 13.sp)
                            Text("Delivery Address: ${order.deliveryAddress}", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Payment Method: ${order.paymentMethod} (Status: ${order.paymentStatus})", fontSize = 13.sp)
                            Text("Est Price: ${order.totalPrice.toInt()} EGP", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons dependant on Portal Role
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        appViewModel.selectActiveChatOrder(order.id)
                                        onOpenChat()
                                    },
                                    modifier = Modifier.testTag("chat_shortcut_${order.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = "", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Chat", fontSize = 12.sp)
                                }

                                if (currentUser?.role == "TAILOR") {
                                    if (order.status == "PENDING") {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                appViewModel.updateOrderStatusByTailor(order.id, "IN_PROGRESS", completionDays = 4)
                                            },
                                            modifier = Modifier.testTag("action_tailor_start_${order.id}")
                                        ) {
                                            Text("Start Crafting")
                                        }
                                    } else if (order.status == "IN_PROGRESS") {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                appViewModel.updateOrderStatusByTailor(order.id, "READY")
                                            },
                                            modifier = Modifier.testTag("action_tailor_ready_${order.id}")
                                        ) {
                                            Text("Mark Finished & Ironed")
                                        }
                                    }
                                }

                                if (currentUser?.role == "DELIVERY") {
                                    if (order.status == "OUT_FOR_DELIVERY") {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                appViewModel.markOrderAsDelivered(order.id)
                                            },
                                            modifier = Modifier.testTag("action_delivery_complete_${order.id}")
                                        ) {
                                            Text("Mark Delivered")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// CHAT / MESSAGING SYSTEM SCREEN
// ----------------------------------------------------
@Composable
fun ChatPortal(
    appViewModel: AppViewModel,
    onBack: () -> Unit
) {
    val activeMessages by appViewModel.activeChatMessages.collectAsState()
    val currentUser by appViewModel.currentUser.collectAsState()
    val activeOrderId by appViewModel.activeOrderIdForChat.collectAsState()

    var chatInputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "Back")
            }
            Text(
                "Order Chat #${activeOrderId ?: ""}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.width(36.dp))
        }

        Divider()

        // Message list body
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activeMessages) { message ->
                val myMessage = message.senderId == currentUser?.id
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (myMessage) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (myMessage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = message.content,
                                color = if (myMessage) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Input bottom bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatInputText,
                onValueChange = { chatInputText = it },
                placeholder = { Text("Write your requirements details to tailor...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    appViewModel.sendChatMessage(chatInputText)
                    chatInputText = ""
                },
                modifier = Modifier.testTag("chat_send_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

// ----------------------------------------------------
// TAILOR PORTAL & WORKSHOP DASHBOARD
// ----------------------------------------------------
@Composable
fun TailorDashboard(
    appViewModel: AppViewModel,
    onNavigateToOrderDetails: () -> Unit
) {
    val orders by appViewModel.tailorOrders.collectAsState()
    val completedOrders = orders.filter { it.status == "COMPLETED" }
    val activeOrdersCount = orders.count { it.status != "COMPLETED" }

    val earnings = completedOrders.sumOf { it.totalPrice }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Earnings and Stats header row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Cleared Earnings (المكاسب المصروفة)", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    Text(
                        "${earnings.toInt()} EGP",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pending Craft", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text("$activeOrdersCount Tasks", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Stitched garments", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text("${completedOrders.size} Items", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }

        item {
            Text("Assigned Crafting Queue", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }

        if (orders.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("Queue currently clear. Toggle CUSTOMER portal to send dummy custom orders!")
                }
            }
        } else {
            items(orders) { order ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToOrderDetails() }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(order.orderNumber, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("STATUS: ${order.status}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Category: ${order.category} - ${order.templateName}", fontWeight = FontWeight.SemiBold)
                        Text(text = "Client notes: ${order.detailNotes}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        ServiceProgressIndicator(status = order.status)
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceProgressIndicator(status: String) {
    val progress = when (status) {
        "PENDING" -> 0.2f
        "IN_PROGRESS" -> 0.6f
        "READY" -> 0.8f
        "COMPLETED" -> 1.0f
        else -> 0.1f
    }
    Column {
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(2.dp))
        Text("Stitching Progression: ${(progress * 100).toInt()}%", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
    }
}

// ----------------------------------------------------
// MATERIAL SUPPLIER PORTAL (FABRIC READ/WRITE CRUD)
// ----------------------------------------------------
@Composable
fun SupplierPortal(appViewModel: AppViewModel) {
    val fabrics by appViewModel.fabricsList.collectAsState()
    var showingAddFabricDialog by remember { mutableStateOf(false) }

    // Fabric fields
    var fabName by remember { mutableStateOf("") }
    var fabCategory by remember { mutableStateOf("Cotton") }
    var fabColor by remember { mutableStateOf("") }
    var fabPrice by remember { mutableStateOf("") }
    var fabStock by remember { mutableStateOf("") }
    var fabDesc by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Casino, contentDescription = "Supplier", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Supplier Fabrics Catalog", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            }

            Button(
                onClick = { showingAddFabricDialog = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("action_add_fabric")
            ) {
                Text("+ Refit Stock")
            }
        }

        Spacer(modifier = Modifier.height(13.dp))

        if (fabrics.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Fabrics inventory empty.")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(fabrics) { fab ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Represent fabric colors visually
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE2E8F0))
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(color = Color(0xFF2C3E50), radius = 12.dp.toPx())
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(fab.name, fontWeight = FontWeight.Bold)
                                Text("Cat: ${fab.category} | Accent: ${fab.color}", style = MaterialTheme.typography.bodySmall)
                                Text("${fab.stockMs} Meters In Stock", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${fab.pricePerMeter.toInt()} EGP/m", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                IconButton(
                                    onClick = { appViewModel.removeFabricItem(fab) },
                                    modifier = Modifier.testTag("delete_fabric_${fab.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ADD FABRIC STOCK DIALOG FORM
    if (showingAddFabricDialog) {
        AlertDialog(
            onDismissRequest = { showingAddFabricDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val price = fabPrice.toDoubleOrNull() ?: 200.0
                        val stock = fabStock.toIntOrNull() ?: 100
                        appViewModel.createFabricItem(fabName, fabCategory, fabColor, price, stock, fabDesc)
                        showingAddFabricDialog = false
                    },
                    modifier = Modifier.testTag("submit_fabric_form")
                ) {
                    Text("Add Stock Drapery")
                }
            },
            dismissButton = { TextButton(onClick = { showingAddFabricDialog = false }) { Text("Cancel") } },
            title = { Text("Refit Fabric Warehouse Stock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = fabName, onValueChange = { fabName = it }, label = { Text("Fabric Display Name") }, modifier = Modifier.testTag("field_fab_name"))
                    OutlinedTextField(value = fabColor, onValueChange = { fabColor = it }, label = { Text("Color Identifier (e.g. Cobalt Blue)") })
                    OutlinedTextField(value = fabPrice, onValueChange = { fabPrice = it }, label = { Text("Price per Meter (EGP)") })
                    OutlinedTextField(value = fabStock, onValueChange = { fabStock = it }, label = { Text("Stock Yards (Meters)") })
                    OutlinedTextField(value = fabDesc, onValueChange = { fabDesc = it }, label = { Text("Drape characteristics description...") })
                }
            }
        )
    }
}

// ----------------------------------------------------
// COURIER/DELIVERY AGENT PORTAL (ROLE 4)
// ----------------------------------------------------
@Composable
fun DeliveryPortal(appViewModel: AppViewModel) {
    val assignedDeliveries by appViewModel.deliveryOrders.collectAsState()
    val availablePickups by appViewModel.unassignedReadyOrders.collectAsState()

    var activeTab by remember { mutableStateOf("Assigned") } // "Assigned" or "Pickups"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = "Courier", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delivery Network Dispatch", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(13.dp))

        TabRow(selectedTabIndex = if (activeTab == "Assigned") 0 else 1) {
            Tab(selected = activeTab == "Assigned", onClick = { activeTab = "Assigned" }, modifier = Modifier.testTag("tab_assigned")) {
                Text(
                    "My Assignments (${assignedDeliveries.size})",
                    modifier = Modifier.padding(vertical = 12.dp),
                    fontWeight = FontWeight.Bold
                )
            }
            Tab(selected = activeTab == "Pickups", onClick = { activeTab = "Pickups" }, modifier = Modifier.testTag("tab_pickups")) {
                Text(
                    "Available Pickups (${availablePickups.size})",
                    modifier = Modifier.padding(vertical = 12.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(13.dp))

        if (activeTab == "Assigned") {
            if (assignedDeliveries.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No deliveries assigned. Switch to Pickups tab to accept jobs!")
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(assignedDeliveries) { delivery ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(delivery.orderNumber, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    Text("STATUS: ${delivery.status}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Parcel: ${delivery.category} | ${delivery.templateName}", fontWeight = FontWeight.SemiBold)
                                Text("Deliver to: ${delivery.deliveryAddress}", style = MaterialTheme.typography.bodyMedium)
                                Text("Notes: ${delivery.deliveryNotes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Button(
                                        onClick = { appViewModel.markOrderAsDelivered(delivery.id) },
                                        modifier = Modifier.testTag("complete_job_${delivery.id}")
                                    ) {
                                        Text("Mark Hand-Delivered")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (availablePickups.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No packages currently ready at Cairo workshops.")
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(availablePickups) { pickup ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(pickup.orderNumber, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("${pickup.totalPrice.toInt()} EGP COD", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Collect from: workshop Marco Rossi (Al Ghoureya, El Azhar)", fontSize = 13.sp)
                                Text("Deliver to address: ${pickup.deliveryAddress}", fontSize = 13.sp)

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Button(
                                        onClick = { appViewModel.acceptPickupJob(pickup.id) },
                                        modifier = Modifier.testTag("accept_pickup_${pickup.id}")
                                    ) {
                                        Text("Accept & Collect Pickup")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreenLayout(userLanguage: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Elegant pulsing logo card
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(32.dp)),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "App Logo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dynamic Localization Titles
            Text(
                text = com.example.ui.Localization.get("app_title", userLanguage),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = com.example.ui.Localization.get("app_subtitle", userLanguage),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading index indicator
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.secondary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (userLanguage == "ar") "صُنع بكل فخر وحرفية غالية" else "Crafted with bespoke passion",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
