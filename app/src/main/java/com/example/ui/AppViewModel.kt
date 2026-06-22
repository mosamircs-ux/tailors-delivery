package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.AIDesignResult
import com.example.network.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(database)

    companion object {
        private const val TAG = "AppViewModel"
    }

    // State of database initialization
    private val _dbInitialized = MutableStateFlow(false)
    val dbInitialized: StateFlow<Boolean> = _dbInitialized

    // Dynamic settings for localization and theme
    private val _userLanguage = MutableStateFlow("en")
    val userLanguage: StateFlow<String> = _userLanguage

    private val _themeMode = MutableStateFlow("system") // "system", "light", "dark"
    val themeMode: StateFlow<String> = _themeMode

    fun setUserLanguage(lang: String) {
        _userLanguage.value = lang
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
    }

    // Dynamic Switch Account logic: alex@fashionforge.com, marco@fashionforge.com, priya@fashionforge.com, karim@fashionforge.com
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser

    // Expose flows directly from Room Database
    val usersList: StateFlow<List<UserEntity>> = repository.users
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrdersList: StateFlow<List<OrderEntity>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fabricsList: StateFlow<List<FabricEntity>> = repository.fabrics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val marketplaceDesignsList: StateFlow<List<MarketplaceDesignEntity>> = repository.marketplaceDesigns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered lists based on current role and logged in user
    val customerOrders: StateFlow<List<OrderEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null && user.role == "CUSTOMER") {
                repository.getOrdersByCustomer(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tailorOrders: StateFlow<List<OrderEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null && user.role == "TAILOR") {
                repository.getOrdersByTailor(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deliveryOrders: StateFlow<List<OrderEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null && user.role == "DELIVERY") {
                repository.getOrdersByDeliveryPartner(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unassignedReadyOrders: StateFlow<List<OrderEntity>> = allOrdersList
        .map { orders ->
            orders.filter { it.status == "READY" && it.deliveryPartnerId == null }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat handling
    private val _activeOrderIdForChat = MutableStateFlow<Int?>(null)
    val activeOrderIdForChat: StateFlow<Int?> = _activeOrderIdForChat

    val activeChatMessages: StateFlow<List<MessageEntity>> = _activeOrderIdForChat
        .flatMapLatest { orderId ->
            if (orderId != null) {
                repository.getMessagesForOrder(orderId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications list
    val currentNotifications: StateFlow<List<NotificationEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getNotificationsForUser(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Studio Design Generator State
    private val _aiGenerationLoading = MutableStateFlow(false)
    val aiGenerationLoading: StateFlow<Boolean> = _aiGenerationLoading

    private val _aiGenResult = MutableStateFlow<AIDesignResult?>(null)
    val aiGenResult: StateFlow<AIDesignResult?> = _aiGenResult

    init {
        viewModelScope.launch {
            try {
                _dbInitialized.value = false
                repository.seedDatabaseIfEmpty()
                _dbInitialized.value = true

                // Default login: Alex Rivera (Customer)
                val defaultCustomer = repository.getUserByEmail("alex@fashionforge.com")
                if (defaultCustomer != null) {
                    _currentUser.value = defaultCustomer
                } else {
                    Log.e(TAG, "Default customer was not seeded!")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed seeding and initializing: ${e.message}", e)
            }
        }
    }

    // Switch Account Portal (Floating Picker / Mock Login switcher)
    fun switchUserRole(role: String) {
        viewModelScope.launch {
            val email = when (role.uppercase()) {
                "CUSTOMER" -> "alex@fashionforge.com"
                "TAILOR" -> "marco@fashionforge.com"
                "SUPPLIER" -> "priya@fashionforge.com"
                "DELIVERY" -> "karim@fashionforge.com"
                else -> "alex@fashionforge.com"
            }
            val user = repository.getUserByEmail(email)
            if (user != null) {
                _currentUser.value = user
                Log.d(TAG, "Switched role portal to: ${user.name} (${user.role})")
            }
        }
    }

    fun updateProfile(name: String, phone: String, address: String, bio: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(name = name, phone = phone, address = address, bio = bio)
            repository.updateUser(updated)
            _currentUser.value = updated
        }
    }

    // Place Custom Created Order
    fun placeCustomOrder(
        category: String,
        templateName: String,
        colorHex: String,
        fabricName: String,
        notes: String,
        sizeType: String,
        length: Float,
        width: Float,
        sleeve: Float,
        collar: Float,
        deliveryAddress: String,
        deliveryNotes: String,
        paymentMethod: String,
        price: Double
    ) {
        val customer = _currentUser.value ?: return
        viewModelScope.launch {
            val orderNo = "ORD-${category.take(3).uppercase()}-${(10000..99999).random()}"
            val order = OrderEntity(
                orderNumber = orderNo,
                customerId = customer.id,
                tailorId = 2, // Seeding defaults to Marco Tailor
                supplierId = 3, // Priya Supplier
                category = category,
                templateName = templateName,
                colorHex = colorHex,
                fabricName = fabricName,
                detailNotes = notes,
                sizeType = sizeType,
                measurementLength = length,
                measurementWidth = width,
                measurementSleeve = sleeve,
                measurementCollar = collar,
                deliveryAddress = deliveryAddress,
                deliveryNotes = deliveryNotes,
                paymentMethod = paymentMethod,
                paymentStatus = if (paymentMethod == "Card") "PAID" else "PENDING",
                paymentReference = if (paymentMethod == "Card") "REF-TXN-${(1000..9999).random()}" else "",
                totalPrice = price,
                status = "PENDING"
            )
            val orderId = repository.insertOrder(order).toInt()

            // Notify Customer
            repository.insertNotification(
                NotificationEntity(
                    userId = customer.id,
                    title = "Order Placed Successfully",
                    message = "Your order $orderNo for $category ($templateName) has been shared with tailor Marco Rossi.",
                    linkType = "ORDER",
                    linkId = orderId
                )
            )

            // Notify Tailor
            repository.insertNotification(
                NotificationEntity(
                    userId = 2, // Marco Rossi
                    title = "New Custom Design Order!",
                    message = "Alex Rivera placed a custom design order $orderNo for a $category.",
                    linkType = "ORDER",
                    linkId = orderId
                )
            )
        }
    }

    // Tailor updates order status
    fun updateOrderStatusByTailor(orderId: Int, newStatus: String, completionDays: Int? = null) {
        viewModelScope.launch {
            val order = repository.getOrderById(orderId) ?: return@launch
            var updated = order.copy(status = newStatus)

            if (completionDays != null && completionDays > 0) {
                updated = updated.copy(estimatedCompletionMs = System.currentTimeMillis() + (86400000L * completionDays))
            }

            repository.updateOrder(updated)

            // Send notification to Customer
            val notificationMessage = when (newStatus) {
                "IN_PROGRESS" -> "Marco Rossi started handcrafting your $orderId garment. Estimated completion set."
                "READY" -> "Your customized clothing item has been finished and ironed! Ready for delivery pickup."
                "COMPLETED" -> "Order completed and delivered! Thank you for trusting Fashion Forge."
                else -> "Your order status was updated to $newStatus."
            }

            repository.insertNotification(
                NotificationEntity(
                    userId = order.customerId,
                    title = "Order Update: $newStatus",
                    message = notificationMessage,
                    linkType = "ORDER",
                    linkId = orderId
                )
            )

            // If updated to ready, notify Delivery service that a pickup has opened
            if (newStatus == "READY") {
                val drivers = repository.users.firstOrNull()?.filter { it.role == "DELIVERY" } ?: emptyList()
                for (driver in drivers) {
                    repository.insertNotification(
                        NotificationEntity(
                            userId = driver.id,
                            title = "New Delivery Job Available!",
                            message = "Order ${order.orderNumber} is finished at the tailor and ready for collection.",
                            linkType = "ORDER",
                            linkId = orderId
                        )
                    )
                }
            }
        }
    }

    // Delivery accepting and marking delivered
    fun acceptPickupJob(orderId: Int) {
        val deliveryPartner = _currentUser.value ?: return
        if (deliveryPartner.role != "DELIVERY") return
        viewModelScope.launch {
            val order = repository.getOrderById(orderId) ?: return@launch
            val updated = order.copy(
                deliveryPartnerId = deliveryPartner.id,
                status = "OUT_FOR_DELIVERY"
            )
            repository.updateOrder(updated)

            // Notify Customer & Tailor
            repository.insertNotification(
                NotificationEntity(
                    userId = order.customerId,
                    title = "Package Shipped!",
                    message = "Driver ${deliveryPartner.name} has picked up your clothing package and is on their way.",
                    linkType = "ORDER",
                    linkId = orderId
                )
            )

            repository.insertNotification(
                NotificationEntity(
                    userId = order.tailorId ?: 2,
                    title = "Package Picked Up",
                    message = "Driver Karim Hassan picked up order ${order.orderNumber} for delivery.",
                    linkType = "ORDER",
                    linkId = orderId
                )
            )
        }
    }

    fun markOrderAsDelivered(orderId: Int) {
        viewModelScope.launch {
            val order = repository.getOrderById(orderId) ?: return@launch
            val updated = order.copy(
                status = "COMPLETED",
                paymentStatus = "PAID" // Cash on delivery gets settled
            )
            repository.updateOrder(updated)

            // Notify Customer & Tailor
            repository.insertNotification(
                NotificationEntity(
                    userId = order.customerId,
                    title = "Parcel Delivered Successfully",
                    message = "Your custom tailoring parcel was safely delivered to your doorstep. Enjoy your Fashion Forge fit!",
                    linkType = "ORDER",
                    linkId = orderId
                )
            )

            repository.insertNotification(
                NotificationEntity(
                    userId = order.tailorId ?: 2,
                    title = "Order Successfully Delivered",
                    message = "Order ${order.orderNumber} was marked as delivered by the driver. Earnings credited.",
                    linkType = "ORDER",
                    linkId = orderId
                )
            )
        }
    }

    // Chat management
    fun selectActiveChatOrder(orderId: Int) {
        _activeOrderIdForChat.value = orderId
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.markMessagesAsRead(orderId, user.id)
        }
    }

    fun sendChatMessage(content: String) {
        val sender = _currentUser.value ?: return
        val orderId = _activeOrderIdForChat.value ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            val order = repository.getOrderById(orderId) ?: return@launch
            // Determine receiver based on sender
            val receiverId = if (sender.id == order.customerId) {
                order.tailorId ?: 2
            } else {
                order.customerId
            }

            database.messageDao().insertMessage(
                MessageEntity(
                    senderId = sender.id,
                    receiverId = receiverId,
                    orderId = orderId,
                    content = content
                )
            )

            // Simple system responsive automatic reply for test convenience if tailor is receiver
            if (receiverId == 2) {
                repository.insertNotification(
                    NotificationEntity(
                        userId = 2,
                        title = "New Message from Alex",
                        message = content,
                        linkType = "CHAT",
                        linkId = orderId
                    )
                )
            } else {
                repository.insertNotification(
                    NotificationEntity(
                        userId = order.customerId,
                        title = "New Message from Tailor",
                        message = content,
                        linkType = "CHAT",
                        linkId = orderId
                    )
                )
            }
        }
    }

    // Fabric Supplier Actions
    fun createFabricItem(name: String, category: String, color: String, price: Double, stock: Int, desc: String) {
        val user = _currentUser.value ?: return
        if (user.role != "SUPPLIER") return
        viewModelScope.launch {
            repository.insertFabric(
                FabricEntity(
                    supplierId = user.id,
                    name = name,
                    category = category,
                    color = color,
                    pricePerMeter = price,
                    stockMs = stock,
                    description = desc
                )
            )
        }
    }

    fun modifyFabricItem(fabric: FabricEntity) {
        viewModelScope.launch {
            repository.updateFabric(fabric)
        }
    }

    fun removeFabricItem(fabric: FabricEntity) {
        viewModelScope.launch {
            repository.deleteFabric(fabric)
        }
    }

    // Marketplace Actions
    fun purchaseMarketplaceSelection(designId: Int, shippingAddress: String) {
        val customer = _currentUser.value ?: return
        viewModelScope.launch {
            val designs = marketplaceDesignsList.value
            val design = designs.find { it.id == designId } ?: return@launch

            // Create customized order representing this purchased design
            val orderNo = "ORD-MKT-${(10000..99999).random()}"
            val order = OrderEntity(
                orderNumber = orderNo,
                customerId = customer.id,
                tailorId = design.designerId, // Design owner (Marco Tailor or other)
                supplierId = 3, // Supplier
                category = design.category,
                templateName = "Marketplace Design: ${design.title}",
                colorHex = "2C3E50", // Slate default
                fabricName = "Egyptian Cotton Premium", // default premium
                detailNotes = "Purchased design: ${design.description}",
                sizeType = "M", // default sizing
                measurementLength = 70f,
                measurementWidth = 52f,
                status = "PENDING",
                deliveryAddress = shippingAddress,
                paymentStatus = "PAID",
                paymentMethod = "Card",
                paymentReference = "REF-MKT-${(10000..99999).random()}",
                totalPrice = design.price + 50.0 // + 50 shipping
            )
            val orderId = repository.insertOrder(order).toInt()

            // Update design as sold, set buyer
            repository.updateMarketplaceDesign(design.copy(isSold = true, buyerId = customer.id))

            repository.insertNotification(
                NotificationEntity(
                    userId = customer.id,
                    title = "Design Purchased!",
                    message = "You bought '${design.title}' for ${design.price} EGP. It is now registered as custom order $orderNo.",
                    linkType = "ORDER",
                    linkId = orderId
                )
            )

            repository.insertNotification(
                NotificationEntity(
                    userId = design.designerId,
                    title = "Design Sold!",
                    message = "Alex Rivera purchased your design listed inside the Marketplace: '${design.title}'!",
                    linkType = "ORDER",
                    linkId = orderId
                )
            )
        }
    }

    fun createMarketplaceDesign(title: String, desc: String, category: String, price: Double) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.insertMarketplaceDesign(
                MarketplaceDesignEntity(
                    designerId = user.id,
                    designerName = user.name,
                    title = title,
                    description = desc,
                    category = category,
                    price = price,
                    imageResName = category.lowercase()
                )
            )
        }
    }

    fun deleteMarketplaceDesign(designId: Int) {
        viewModelScope.launch {
            repository.deleteDesignById(designId)
        }
    }

    // AI Design Generation using Gemini SDK / direct REST client
    fun triggerAIDesign(occasion: String, style: String, bodyType: String, fabric: String, budget: Double, colors: List<String>) {
        viewModelScope.launch {
            _aiGenerationLoading.value = true
            _aiGenResult.value = null
            try {
                val result = GeminiClient.generateFashionDesign(occasion, style, bodyType, fabric, budget, colors)
                _aiGenResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Error generating fashion item design: ${e.message}", e)
            } finally {
                _aiGenerationLoading.value = false
            }
        }
    }

    fun triggerAIDesignSketch(promptText: String) {
        viewModelScope.launch {
            _aiGenerationLoading.value = true
            _aiGenResult.value = null
            try {
                val result = GeminiClient.generateFashionSketch(promptText)
                _aiGenResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Error generating fashion sketch: ${e.message}", e)
            } finally {
                _aiGenerationLoading.value = false
            }
        }
    }

    fun clearAIGeneratorResult() {
        _aiGenResult.value = null
    }

    fun dismissNotifications() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.markAllNotificationsAsRead(user.id)
        }
    }

    fun createSampleSwatchInquiry(fabricName: String, metres: Double, notes: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.insertNotification(
                NotificationEntity(
                    userId = user.id,
                    title = "Swatch Reserved: $fabricName",
                    message = "Your request for $metres meters of $fabricName has been sent to Priya Nair. Notes: $notes",
                    linkType = "SUPPLIER_STOCK",
                    linkId = 0
                )
            )
        }
    }
}
