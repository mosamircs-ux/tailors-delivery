package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Int): OrderEntity?

    @Query("SELECT * FROM orders WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getOrdersByCustomer(customerId: Int): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE tailorId = :tailorId ORDER BY timestamp DESC")
    fun getOrdersByTailor(tailorId: Int): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE deliveryPartnerId = :partnerId ORDER BY timestamp DESC")
    fun getOrdersByDeliveryPartner(partnerId: Int): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE status = :status ORDER BY timestamp DESC")
    fun getOrdersByStatus(status: String): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)
}

@Dao
interface FabricDao {
    @Query("SELECT * FROM fabrics ORDER BY id DESC")
    fun getAllFabrics(): Flow<List<FabricEntity>>

    @Query("SELECT * FROM fabrics WHERE supplierId = :supplierId ORDER BY id DESC")
    fun getFabricsBySupplier(supplierId: Int): Flow<List<FabricEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFabric(fabric: FabricEntity): Long

    @Update
    suspend fun updateFabric(fabric: FabricEntity)

    @Delete
    suspend fun deleteFabric(fabric: FabricEntity)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE orderId = :orderId ORDER BY timestamp ASC")
    fun getMessagesForOrder(orderId: Int): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("UPDATE messages SET isRead = 1 WHERE orderId = :orderId AND receiverId = :userId")
    suspend fun markMessagesAsRead(orderId: Int, userId: Int)
}

@Dao
interface MarketplaceDesignDao {
    @Query("SELECT * FROM marketplace_designs ORDER BY id DESC")
    fun getAllDesigns(): Flow<List<MarketplaceDesignEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDesign(design: MarketplaceDesignEntity): Long

    @Update
    suspend fun updateDesign(design: MarketplaceDesignEntity)

    @Query("DELETE FROM marketplace_designs WHERE id = :id")
    suspend fun deleteDesignById(id: Int)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: Int): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: Int)
}
