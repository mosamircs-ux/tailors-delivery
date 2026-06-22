package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val role: String, // "CUSTOMER", "TAILOR", "SUPPLIER", "DELIVERY"
    val phone: String = "",
    val address: String = "",
    val bio: String = ""
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderNumber: String,
    val customerId: Int,
    val tailorId: Int? = null,
    val supplierId: Int? = null,
    val deliveryPartnerId: Int? = null,
    val category: String, // "Shirt", "Pants", "Dress", "Suit", "Jacket", "Accessories"
    val templateName: String,
    val colorHex: String,
    val fabricName: String,
    val detailNotes: String = "",
    // Measurements
    val sizeType: String = "M", // "S", "M", "L", "XL", "XXL", "Custom"
    val measurementLength: Float = 0f,
    val measurementWidth: Float = 0f,
    val measurementSleeve: Float = 0f,
    val measurementCollar: Float = 0f,
    // Status
    val status: String = "PENDING", // "PENDING", "IN_PROGRESS", "READY", "OUT_FOR_DELIVERY", "COMPLETED"
    // Delivery
    val deliveryAddress: String = "",
    val deliveryNotes: String = "",
    // Payment
    val paymentStatus: String = "PENDING", // "PENDING", "PAID"
    val paymentMethod: String = "COD", // "Card", "COD"
    val paymentReference: String = "",
    val totalPrice: Double = 0.0,
    // Timeline
    val estimatedCompletionMs: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "fabrics")
data class FabricEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val supplierId: Int,
    val name: String,
    val category: String, // "Cotton", "Wool", "Silk", "Linen", "Denim"
    val color: String,
    val pricePerMeter: Double,
    val stockMs: Int,
    val description: String = ""
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: Int,
    val receiverId: Int,
    val orderId: Int,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "marketplace_designs")
data class MarketplaceDesignEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val designerId: Int,
    val designerName: String,
    val title: String,
    val description: String,
    val category: String,
    val price: Double,
    val imageResName: String,
    val isSold: Boolean = false,
    val buyerId: Int? = null
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val linkType: String = "", // "ORDER", "CHAT"
    val linkId: Int = 0
)
