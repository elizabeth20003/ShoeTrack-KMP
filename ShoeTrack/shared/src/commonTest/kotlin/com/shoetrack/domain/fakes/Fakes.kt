package com.shoetrack.domain.fakes

import com.shoetrack.domain.model.Customer
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.LayawaySummary
import com.shoetrack.domain.model.OverdueReminder
import com.shoetrack.domain.model.OverduePolicy
import com.shoetrack.domain.model.Payment
import com.shoetrack.domain.model.Product
import com.shoetrack.domain.model.SyncSnapshot
import com.shoetrack.domain.repository.CustomerRepository
import com.shoetrack.domain.repository.LayawayRepository
import com.shoetrack.domain.repository.ProductRepository
import com.shoetrack.domain.repository.SyncRepository
import com.shoetrack.domain.service.IdGenerator
import com.shoetrack.domain.service.OverdueReminderScheduler
import com.shoetrack.domain.service.PhotoStorage
import com.shoetrack.domain.sync.SyncEngine
import com.shoetrack.domain.sync.SyncStatus
import com.shoetrack.domain.usecase.GetLayawayDetailUseCase
import com.shoetrack.domain.usecase.ScheduleOverdueReminderUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Clock
import kotlin.time.Instant

class FakeLayawayRepository : LayawayRepository {
    val layaways = mutableMapOf<String, Layaway>()
    val payments = mutableListOf<Payment>()

    /** When set, every write throws it, to simulate a storage failure. */
    var writeFailure: Exception? = null

    /** When set, observing summaries throws it. */
    var observeFailure: Exception? = null

    override suspend fun create(layaway: Layaway) {
        writeFailure?.let { throw it }
        layaways[layaway.id] = layaway
    }

    override suspend fun registerPayment(layaway: Layaway, payment: Payment) {
        writeFailure?.let { throw it }
        payments += payment
        layaways[layaway.id] = layaway
    }

    override suspend fun updateState(layaway: Layaway) {
        layaways[layaway.id] = layaway
    }

    override suspend fun getById(id: String): Layaway? = layaways[id]

    override suspend fun getByState(state: LayawayState): List<Layaway> =
        layaways.values.filter { it.state == state }

    override fun observeSummaries(): Flow<List<LayawaySummary>> = flow {
        observeFailure?.let { throw it }
        emit(
            layaways.values.map {
                LayawaySummary(
                    id = it.id,
                    customerName = it.customerId,
                    productDescription = it.productId,
                    productPhotoPath = "",
                    remainingBalance = it.remainingBalance,
                    state = it.state,
                    lastPaymentAt = it.lastPaymentAt,
                )
            },
        )
    }

    override suspend fun getPendingSync(): List<Layaway> = layaways.values.toList()
}

class FakeCustomerRepository : CustomerRepository {
    val customers = mutableMapOf<String, Customer>()

    override suspend fun save(customer: Customer) {
        customers[customer.id] = customer
    }

    override suspend fun getById(id: String): Customer? = customers[id]

    override fun observeAll(): Flow<List<Customer>> = flowOf(customers.values.toList())
}

class FakeProductRepository : ProductRepository {
    val products = mutableMapOf<String, Product>()

    override suspend fun save(product: Product) {
        products[product.id] = product
    }

    override suspend fun getById(id: String): Product? = products[id]
}

class FakePhotoStorage : PhotoStorage {
    val photos = mutableMapOf<String, ByteArray>()

    /** When set, saving throws it, to simulate a full disk. */
    var failure: Exception? = null

    override suspend fun save(imageBytes: ByteArray): String {
        failure?.let { throw it }
        val name = "photo-${photos.size + 1}.jpg"
        photos[name] = imageBytes
        return name
    }

    override suspend fun load(name: String): ByteArray? = photos[name]

    override suspend fun exists(name: String): Boolean = name in photos

    override suspend fun saveAs(name: String, imageBytes: ByteArray) {
        photos[name] = imageBytes
    }
}

class SequentialIdGenerator : IdGenerator {
    private var counter = 0

    override fun newId(): String = "id-${++counter}"
}

class FixedClock(private val instant: Instant) : Clock {
    override fun now(): Instant = instant
}

class FakeOverdueReminderScheduler : OverdueReminderScheduler {
    val scheduled = mutableMapOf<String, OverdueReminder>()

    override fun schedule(reminder: OverdueReminder) {
        scheduled[reminder.layawayId] = reminder
    }

    override fun cancel(layawayId: String) {
        scheduled.remove(layawayId)
    }
}

fun scheduleReminderOf(
    layaways: FakeLayawayRepository,
    customers: FakeCustomerRepository,
    products: FakeProductRepository,
    scheduler: OverdueReminderScheduler = FakeOverdueReminderScheduler(),
) = ScheduleOverdueReminderUseCase(
    getLayawayDetail = GetLayawayDetailUseCase(layaways, customers, products),
    scheduler = scheduler,
    policy = OverduePolicy(),
)

/** Reads and writes through the other fakes, so a test sees one consistent "device". */
class FakeSyncRepository(
    private val layaways: FakeLayawayRepository,
    private val customers: FakeCustomerRepository,
    private val products: FakeProductRepository,
) : SyncRepository {

    override suspend fun snapshot() = SyncSnapshot(
        customers = customers.customers.values.toList(),
        products = products.products.values.toList(),
        layaways = layaways.layaways.values.toList(),
    )

    override suspend fun store(changes: SyncSnapshot) {
        changes.customers.forEach { customers.customers.getOrPut(it.id) { it } }
        changes.products.forEach { products.products.getOrPut(it.id) { it } }
        changes.layaways.forEach { layaways.layaways[it.id] = it }
    }
}

class FakeSyncEngine : SyncEngine {
    override val status: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus())

    override fun start() = Unit

    override fun stop() = Unit
}
