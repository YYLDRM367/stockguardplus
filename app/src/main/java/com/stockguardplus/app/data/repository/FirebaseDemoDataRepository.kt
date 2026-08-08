package com.stockguardplus.app.data.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.stockguardplus.app.data.demo.demoDataSetFor
import com.stockguardplus.app.data.local.LocalePreferences
import com.stockguardplus.app.data.model.Company
import com.stockguardplus.app.data.model.OrderLine
import com.stockguardplus.app.data.model.OrderType
import com.stockguardplus.app.data.model.Product
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDemoDataRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val localePreferences: LocalePreferences,
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository,
    private val companyRepository: CompanyRepository,
    private val orderRepository: OrderRepository,
    private val organizationRepository: OrganizationRepository
) : DemoDataRepository {

    override suspend fun seedDemoData() {
        val languageTag = localePreferences.languageTag ?: Locale.getDefault().language
        val demoSet = demoDataSetFor(languageTag)

        val categoryIds = demoSet.categories.map { category ->
            categoryRepository.addCategory(category.name, isDemo = true)
        }

        val partyId = companyRepository.addCompany(
            Company(
                name = demoSet.party.name,
                address = demoSet.party.address,
                phone1 = demoSet.party.phone1,
                email = demoSet.party.email,
                isDemo = true
            )
        )

        // Products start at quantity 0 and only reach their demo quantity by
        // approving a real purchase order — same path a real user's stock
        // takes — so movement history and Reports have real data to show,
        // not just a number written directly onto the product.
        demoSet.products.forEach { demoProduct ->
            val productId = productRepository.addProduct(
                Product(
                    name = demoProduct.name,
                    sku = demoProduct.sku,
                    reorderPoint = demoProduct.reorderPoint,
                    categoryId = categoryIds[demoProduct.categoryIndex],
                    isDemo = true
                )
            )

            if (demoProduct.initialQuantity > 0) {
                val dateMillis = System.currentTimeMillis() -
                    TimeUnit.DAYS.toMillis(demoProduct.orderDaysAgo.toLong())
                val orderId = orderRepository.createOrder(
                    type = OrderType.PURCHASE,
                    dateMillis = dateMillis,
                    invoiceNumber = "",
                    receiptNumber = "",
                    partyId = partyId,
                    lines = listOf(OrderLine(productId = productId, quantity = demoProduct.initialQuantity)),
                    isDemo = true
                )
                orderRepository.approveOrder(orderId)
            }
        }

        organizationRepository.markDemoDataOffered()
    }

    override suspend fun clearDemoData() {
        val orgId = requireNotNull(authRepository.currentOrgId) { "Cannot clear demo data while signed out." }
        val orgRef = firestore.collection("organizations").document(orgId)

        listOf("products", "categories", "parties", "orders", "movements").forEach { collectionName ->
            deleteWhereDemo(orgRef.collection(collectionName))
        }
    }

    private suspend fun deleteWhereDemo(collectionRef: CollectionReference) {
        val snapshot = collectionRef.whereEqualTo("isDemo", true).get().await()
        if (snapshot.isEmpty) return
        snapshot.documents.chunked(450).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { document -> batch.delete(document.reference) }
            batch.commit().await()
        }
    }
}
