package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class AppRepository(private val db: AppDatabase) {
    val users = db.userDao().getAllUsers()
    val allOrders = db.orderDao().getAllOrders()
    val fabrics = db.fabricDao().getAllFabrics()
    val marketplaceDesigns = db.marketplaceDesignDao().getAllDesigns()

    suspend fun getUserById(id: Int): UserEntity? = db.userDao().getUserById(id)
    suspend fun getUserByEmail(email: String): UserEntity? = db.userDao().getUserByEmail(email)
    suspend fun insertUser(user: UserEntity): Long = db.userDao().insertUser(user)
    suspend fun updateUser(user: UserEntity) = db.userDao().updateUser(user)

    fun getOrdersByCustomer(customerId: Int): Flow<List<OrderEntity>> = db.orderDao().getOrdersByCustomer(customerId)
    fun getOrdersByTailor(tailorId: Int): Flow<List<OrderEntity>> = db.orderDao().getOrdersByTailor(tailorId)
    fun getOrdersByDeliveryPartner(partnerId: Int): Flow<List<OrderEntity>> = db.orderDao().getOrdersByDeliveryPartner(partnerId)
    suspend fun getOrderById(id: Int): OrderEntity? = db.orderDao().getOrderById(id)
    suspend fun insertOrder(order: OrderEntity): Long = db.orderDao().insertOrder(order)
    suspend fun updateOrder(order: OrderEntity) = db.orderDao().updateOrder(order)

    fun getFabricsBySupplier(supplierId: Int): Flow<List<FabricEntity>> = db.fabricDao().getFabricsBySupplier(supplierId)
    suspend fun insertFabric(fabric: FabricEntity): Long = db.fabricDao().insertFabric(fabric)
    suspend fun updateFabric(fabric: FabricEntity) = db.fabricDao().updateFabric(fabric)
    suspend fun deleteFabric(fabric: FabricEntity) = db.fabricDao().deleteFabric(fabric)

    fun getMessagesForOrder(orderId: Int): Flow<List<MessageEntity>> = db.messageDao().getMessagesForOrder(orderId)
    suspend fun insertMessage(message: MessageEntity): Long = db.messageDao().insertMessage(message)
    suspend fun markMessagesAsRead(orderId: Int, userId: Int) = db.messageDao().markMessagesAsRead(orderId, userId)

    suspend fun insertMarketplaceDesign(design: MarketplaceDesignEntity): Long = db.marketplaceDesignDao().insertDesign(design)
    suspend fun updateMarketplaceDesign(design: MarketplaceDesignEntity) = db.marketplaceDesignDao().updateDesign(design)
    suspend fun deleteDesignById(id: Int) = db.marketplaceDesignDao().deleteDesignById(id)

    fun getNotificationsForUser(userId: Int): Flow<List<NotificationEntity>> = db.notificationDao().getNotificationsForUser(userId)
    suspend fun insertNotification(notification: NotificationEntity): Long = db.notificationDao().insertNotification(notification)
    suspend fun markAllNotificationsAsRead(userId: Int) = db.notificationDao().markAllAsRead(userId)

    suspend fun seedDatabaseIfEmpty() {
        val existingUsers = users.firstOrNull()
        if (!existingUsers.isNullOrEmpty()) return // database is already seeded

        // 1. Seed Users (Alex Customer, Marco Tailor, Priya Supplier, Karim Delivery)
        val customerId = db.userDao().insertUser(
            UserEntity(
                name = "Alex Rivera",
                email = "alex@fashionforge.com",
                role = "CUSTOMER",
                phone = "+20 100 123 4567",
                address = "5 El Maadi St, Cairo, Egypt",
                bio = "Urban style enthusiast who loves fit-perfect tailoring."
            )
        ).toInt()

        val tailorId = db.userDao().insertUser(
            UserEntity(
                name = "Marco Rossi",
                email = "marco@fashionforge.com",
                role = "TAILOR",
                phone = "+20 110 987 6543",
                address = "12 El ghoureya St, El Azhar, Cairo, Egypt",
                bio = "Traditional & modern master tailor with 20+ years craftsmanship."
            )
        ).toInt()

        val supplierId = db.userDao().insertUser(
            UserEntity(
                name = "Priya Nair",
                email = "priya@fashionforge.com",
                role = "SUPPLIER",
                phone = "+20 122 555 8899",
                address = "32 Industrial Zone, El Mahalla, Egypt",
                bio = "Supplier of luxury Egyptian Cotton and premium fabrics."
            )
        ).toInt()

        val deliveryId = db.userDao().insertUser(
            UserEntity(
                name = "Karim Hassan",
                email = "karim@fashionforge.com",
                role = "DELIVERY",
                phone = "+20 155 333 4444",
                address = "18 Ramses El Thany, Downtown, Cairo, Egypt",
                bio = "Fast, secure local delivery provider specialized in clothing handling."
            )
        ).toInt()

        // 2. Seed Fabrics (Supplier inventory)
        db.fabricDao().insertFabric(
            FabricEntity(
                supplierId = supplierId,
                name = "Egyptian Cotton Premium",
                category = "Cotton",
                color = "Off-White #FFFFFF",
                pricePerMeter = 240.0,
                stockMs = 500,
                description = "Extra-long-staple luxurious cotton. Breathable, durable, and extremely soft."
            )
        )
        db.fabricDao().insertFabric(
            FabricEntity(
                supplierId = supplierId,
                name = "Italian Premium Wool",
                category = "Wool",
                color = "Charcoal Navy #2F3E46",
                pricePerMeter = 480.0,
                stockMs = 200,
                description = "Fine Merino wool, ideal for rich customized corporate suits."
            )
        )
        db.fabricDao().insertFabric(
            FabricEntity(
                supplierId = supplierId,
                name = "Belgian Pure Linen",
                category = "Linen",
                color = "Sand Beige #E0D6C8",
                pricePerMeter = 310.0,
                stockMs = 350,
                description = "Natural eco-friendly crisp linen. Perfect for summer shirts and light dresses."
            )
        )
        db.fabricDao().insertFabric(
            FabricEntity(
                supplierId = supplierId,
                name = "Mulberry Silk",
                category = "Silk",
                color = "Crimson Red #9E2A2B",
                pricePerMeter = 650.0,
                stockMs = 120,
                description = "Exquisite premium shiny silk suitable for formal gowns and designer wear."
            )
        )
        db.fabricDao().insertFabric(
            FabricEntity(
                supplierId = supplierId,
                name = "Heavy Indigo Denim",
                category = "Denim",
                color = "Midnight Blue #1A2536",
                pricePerMeter = 190.0,
                stockMs = 400,
                description = "Sturdy rich-dyed raw indigo denim for casual jackets and modern utility trousers."
            )
        )

        // 3. Seed Orders (Alex's active and historical orders)
        // Order 1: Completed Linen Suit
        val o1Id = db.orderDao().insertOrder(
            OrderEntity(
                orderNumber = "ORD-${1000 + (10..99).random()}",
                customerId = customerId,
                tailorId = tailorId,
                supplierId = supplierId,
                deliveryPartnerId = deliveryId,
                category = "Suit",
                templateName = "Classic Breathable Summer Suit",
                colorHex = "E0D6C8", // Sand Sand
                fabricName = "Belgian Pure Linen",
                detailNotes = "Make the jacket unlined with notch lapels and slim trousers.",
                sizeType = "M",
                measurementLength = 72f,
                measurementWidth = 52f,
                measurementSleeve = 63f,
                measurementCollar = 41f,
                status = "COMPLETED",
                deliveryAddress = "5 El Maadi St, Cairo",
                deliveryNotes = "Call before delivery. Ring the second-floor bell.",
                paymentStatus = "PAID",
                paymentMethod = "Card",
                paymentReference = "REF-29210-CC",
                totalPrice = 3800.0,
                estimatedCompletionMs = System.currentTimeMillis() - 86400000 * 3,
                timestamp = System.currentTimeMillis() - 86400000 * 10
            )
        ).toInt()

        // Order 2: In-Progress Dress
        val o2Id = db.orderDao().insertOrder(
            OrderEntity(
                orderNumber = "ORD-${2000 + (10..99).random()}",
                customerId = customerId,
                tailorId = tailorId,
                supplierId = supplierId,
                category = "Dress",
                templateName = "A-Line Silk Evening Gown",
                colorHex = "9E2A2B", // Crimson
                fabricName = "Mulberry Silk",
                detailNotes = "Adjust sleeve length to be slightly shorter. Tailor the waist perfectly.",
                sizeType = "Custom",
                measurementLength = 110f,
                measurementWidth = 46f,
                measurementSleeve = 58f,
                measurementCollar = 38f,
                status = "IN_PROGRESS",
                deliveryAddress = "5 El Maadi St, Cairo",
                deliveryNotes = "Deliver to doorman if I am not home.",
                paymentStatus = "PENDING",
                paymentMethod = "COD",
                totalPrice = 4200.0,
                estimatedCompletionMs = System.currentTimeMillis() + 86400000 * 4,
                timestamp = System.currentTimeMillis() - 86400000 * 2
            )
        ).toInt()

        // Order 3: Pending Shirt
        val o3Id = db.orderDao().insertOrder(
            OrderEntity(
                orderNumber = "ORD-${3000 + (10..99).random()}",
                customerId = customerId,
                tailorId = tailorId,
                supplierId = supplierId,
                category = "Shirt",
                templateName = "Oxford Button-Down Comfort Shirt",
                colorHex = "FFFFFF", // Off-white
                fabricName = "Egyptian Cotton Premium",
                detailNotes = "Include standard breast pocket on left, classic spread collar.",
                sizeType = "M",
                measurementLength = 74f,
                measurementWidth = 54f,
                measurementSleeve = 64f,
                measurementCollar = 42f,
                status = "PENDING",
                deliveryAddress = "5 El Maadi St, Cairo",
                paymentStatus = "PENDING",
                paymentMethod = "COD",
                totalPrice = 1450.0,
                timestamp = System.currentTimeMillis() - 43200000
            )
        ).toInt()

        // 4. Seed Messages for Order 2
        db.messageDao().insertMessage(
            MessageEntity(
                senderId = customerId,
                receiverId = tailorId,
                orderId = o2Id,
                content = "Hello master Marco! I placed this order for the silk dress. Can you please confirm the waist dimensions?",
                timestamp = System.currentTimeMillis() - 86400000
            )
        )
        db.messageDao().insertMessage(
            MessageEntity(
                senderId = tailorId,
                receiverId = customerId,
                orderId = o2Id,
                content = "Ahla bek, Alex! Yes, I saw your measurements. They look perfect. I ordered the Mulberry Silk from Priya Nair. I'll begin cutting tomorrow!",
                timestamp = System.currentTimeMillis() - 80000000
            )
        )

        // 5. Seed Marketplace Designs
        db.marketplaceDesignDao().insertDesign(
            MarketplaceDesignEntity(
                designerId = tailorId,
                designerName = "Marco Rossi",
                title = "Al-Azhar Royal Suit",
                description = "Classic single-breasted signature suit handcrafted in 100% fine wool. Elegant drape, premium satin inner lining, and robust horn buttons.",
                category = "Suit",
                price = 5400.0,
                imageResName = "suit"
            )
        )
        db.marketplaceDesignDao().insertDesign(
            MarketplaceDesignEntity(
                designerId = tailorId,
                designerName = "Marco Rossi",
                title = "Ghoureya Summer Linen Shirt",
                description = "Light-as-air structural linen shirt with unique asymmetric wooden buttons and a relaxed grandad band collar. Designed for breezy Cairo nights.",
                category = "Shirt",
                price = 1150.0,
                imageResName = "shirt"
            )
        )
        db.marketplaceDesignDao().insertDesign(
            MarketplaceDesignEntity(
                designerId = supplierId,
                designerName = "Priya Nair Collection",
                title = "Royal Alexandria Dress",
                description = "Asymmetric draped dress in sheer Mulberry Crimson Silk. Striking structural fluidity, comfortable stretch fitting, and double stitching.",
                category = "Dress",
                price = 3800.0,
                imageResName = "dress"
            )
        )
        db.marketplaceDesignDao().insertDesign(
            MarketplaceDesignEntity(
                designerId = tailorId,
                designerName = "Marco Rossi",
                title = "Mahalla Winter Trench",
                description = "Warm heavy-weight double-breasted woolen transitional coat. Clean charcoal lapels, broad shoulders, and deep utility handwarmer pockets.",
                category = "Jacket",
                price = 4250.0,
                imageResName = "jacket"
            )
        )

        // 6. Seed initial Notifications
        db.notificationDao().insertNotification(
            NotificationEntity(
                userId = customerId,
                title = "Order Accepted!",
                message = "Marco Rossi accepted your Classic Linen Suit order. Estimated delivery date setup pending.",
                linkType = "ORDER",
                linkId = o1Id
            )
        )
        db.notificationDao().insertNotification(
            NotificationEntity(
                userId = customerId,
                title = "New Message from Tailor!",
                message = "Marco sent you a message regarding your Silk Evening Gown order.",
                linkType = "ORDER",
                linkId = o2Id
            )
        )
    }
}
