package com.movofeeds.routes

import com.movofeeds.models.CreateProductRequest
import com.movofeeds.models.ProductResponse
import com.movofeeds.models.UpdateProductRequest
import com.movofeeds.repository.ProductRecord
import com.movofeeds.security.Permission
import com.movofeeds.security.RbacGuard
import com.movofeeds.service.ProductService
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.core.readBytes
import java.io.File
import java.util.Locale

private val PRODUCT_IMAGE_DIRECTORY =
    File("uploads/products").apply {
        if (!exists()) {
            mkdirs()
        }
    }

private val ALLOWED_IMAGE_EXTENSIONS =
    setOf(
        "jpg",
        "jpeg",
        "png",
        "webp"
    )

/**
 * Resolve the image extension.
 *
 * We first try the filename extension.
 * If Android's gallery provider does not give us a
 * useful filename, we fall back to the MIME type.
 */
private fun resolveImageExtension(
    originalFileName: String?,
    contentType: String?
): String? {

    val fileExtension =
        originalFileName
            ?.substringAfterLast(
                ".",
                ""
            )
            ?.lowercase(Locale.ROOT)

    if (
        fileExtension != null &&
        fileExtension in ALLOWED_IMAGE_EXTENSIONS
    ) {
        return fileExtension
    }

    val mimeType =
        contentType
            ?.lowercase(Locale.ROOT)
            ?.substringBefore(";")
            ?.trim()

    return when (mimeType) {
        "image/jpeg" -> "jpg"
        "image/jpg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> null
    }
}

fun Route.productRoutes(
    productService: ProductService
) {

    // ==========================================
    // PUBLIC PRODUCT CATALOG
    // ==========================================

    get("/api/products") {

        val products =
            productService.getActiveProducts()

        call.respond(
            products.map {
                it.toResponse()
            }
        )
    }

    get("/api/products/{id}") {

        try {

            val id =
                call.parameters["id"]
                    ?.toLongOrNull()
                    ?: throw IllegalArgumentException(
                        "Invalid product ID"
                    )

            val product =
                productService.getProductById(id)

            call.respond(
                product.toResponse()
            )

        } catch (e: IllegalArgumentException) {

            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "status" to "ERROR",
                    "message" to (
                            e.message
                                ?: "Invalid product ID"
                            )
                )
            )

        } catch (e: NoSuchElementException) {

            call.respond(
                HttpStatusCode.NotFound,
                mapOf(
                    "status" to "ERROR",
                    "message" to (
                            e.message
                                ?: "Product not found"
                            )
                )
            )
        }
    }

    // ==========================================
    // PRODUCT IMAGE - PUBLIC READ
    // ==========================================

    get("/api/products/{id}/image") {

        val id =
            call.parameters["id"]
                ?.toLongOrNull()

        if (id == null || id <= 0) {

            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "status" to "ERROR",
                    "message" to "Invalid product ID"
                )
            )

            return@get
        }

        try {

            productService.getProductById(id)

        } catch (e: NoSuchElementException) {

            call.respond(
                HttpStatusCode.NotFound,
                mapOf(
                    "status" to "ERROR",
                    "message" to "Product not found"
                )
            )

            return@get
        }

        val imageFile =
            findProductImage(id)

        if (imageFile == null) {

            call.respond(
                HttpStatusCode.NotFound,
                mapOf(
                    "status" to "ERROR",
                    "message" to "Product image not found"
                )
            )

            return@get
        }

        call.respondFile(imageFile)
    }

    // ==========================================
    // PRODUCT MANAGEMENT
    // ==========================================

    authenticate("auth-jwt") {

        // --------------------------------------
        // CREATE PRODUCT
        // --------------------------------------

        post("/api/products") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.MANAGE_PRODUCTS
                )
            } catch (e: IllegalAccessException) {
                return@post call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Access denied")
                    )
                )
            }

            try {

                val request =
                    call.receive<CreateProductRequest>()

                val product =
                    productService.createProduct(
                        name = request.name,
                        description = request.description,
                        category = request.category,
                        unit = request.unit,
                        price = request.price,
                        stockQuantity =
                            request.stockQuantity
                    )

                call.respond(
                    HttpStatusCode.Created,
                    product.toResponse()
                )

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid product data"
                                )
                    )
                )
            }
        }

        // --------------------------------------
        // UPDATE PRODUCT
        // --------------------------------------

        put("/api/products/{id}") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.MANAGE_PRODUCTS
                )
            } catch (e: IllegalAccessException) {
                return@put call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Access denied")
                    )
                )
            }

            try {

                val id =
                    call.parameters["id"]
                        ?.toLongOrNull()
                        ?: throw IllegalArgumentException(
                            "Invalid product ID"
                        )

                val request =
                    call.receive<UpdateProductRequest>()

                val product =
                    productService.updateProduct(
                        id = id,
                        name = request.name,
                        description = request.description,
                        category = request.category,
                        unit = request.unit,
                        price = request.price,
                        stockQuantity =
                            request.stockQuantity,
                        isActive = request.isActive
                    )

                call.respond(
                    product.toResponse()
                )

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid product data"
                                )
                    )
                )

            } catch (e: NoSuchElementException) {

                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Product not found"
                                )
                    )
                )
            }
        }

        // ======================================
        // UPLOAD / REPLACE PRODUCT IMAGE
        // ======================================

        put("/api/products/{id}/image") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.MANAGE_UPLOADS
                )
            } catch (e: IllegalAccessException) {
                return@put call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Access denied")
                    )
                )
            }

            try {

                val id =
                    call.parameters["id"]
                        ?.toLongOrNull()
                        ?: throw IllegalArgumentException(
                            "Invalid product ID"
                        )

                // ----------------------------------
                // Verify product exists
                // ----------------------------------

                productService.getProductById(id)

                // ----------------------------------
                // Receive multipart upload
                // ----------------------------------

                val multipart =
                    call.receiveMultipart(
                        formFieldLimit =
                            20L * 1024L * 1024L
                    )

                var uploadedFile: File? = null

                multipart.forEachPart { part ->

                    try {

                        if (part is PartData.FileItem) {

                            val originalFileName =
                                part.originalFileName

                            val contentType =
                                part.contentType
                                    ?.toString()

                            val extension =
                                resolveImageExtension(
                                    originalFileName =
                                        originalFileName,
                                    contentType =
                                        contentType
                                ) ?: "jpg"

                            // ----------------------------------
                            // Read the entire multipart body into
                            // memory FIRST, before touching the disk.
                            // ----------------------------------

                            val imageBytes: ByteArray =
                                part.provider()
                                    .readRemaining()
                                    .readBytes()

                            println(
                                "DEBUG UPLOAD: received ${imageBytes.size} bytes " +
                                        "for product $id (originalName=$originalFileName, " +
                                        "contentType=$contentType, extension=$extension)"
                            )

                            require(imageBytes.isNotEmpty()) {
                                "Uploaded image is empty (0 bytes received)"
                            }

                            // ----------------------------------
                            // Ensure upload directory exists.
                            // ----------------------------------

                            if (!PRODUCT_IMAGE_DIRECTORY.exists()) {
                                PRODUCT_IMAGE_DIRECTORY.mkdirs()
                            }

                            require(
                                PRODUCT_IMAGE_DIRECTORY.exists() &&
                                        PRODUCT_IMAGE_DIRECTORY.isDirectory
                            ) {
                                "Image upload directory could not be created: " +
                                        PRODUCT_IMAGE_DIRECTORY.absolutePath
                            }

                            require(PRODUCT_IMAGE_DIRECTORY.canWrite()) {
                                "Image upload directory is not writable: " +
                                        PRODUCT_IMAGE_DIRECTORY.absolutePath
                            }

                            // ----------------------------------
                            // Remove any pre-existing image for this product.
                            // This is safe because we already have the new
                            // bytes in memory.
                            // ----------------------------------

                            deleteExistingProductImages(id)

                            // ----------------------------------
                            // Write DIRECTLY to the final file.
                            //
                            // We deliberately avoid a temporary ".uploading"
                            // file + rename, because on Windows the file
                            // handle from writeBytes may not be released
                            // in time for renameTo() to succeed. A single
                            // writeBytes() call is atomic enough for our
                            // purposes and avoids the whole class of bugs.
                            // ----------------------------------

                            val finalFile =
                                File(
                                    PRODUCT_IMAGE_DIRECTORY,
                                    "$id.$extension"
                                )

                            finalFile.writeBytes(imageBytes)

                            if (
                                !finalFile.exists() ||
                                finalFile.length() == 0L
                            ) {
                                if (finalFile.exists()) {
                                    finalFile.delete()
                                }
                                throw IllegalArgumentException(
                                    "Failed to store uploaded image"
                                )
                            }

                            println(
                                "DEBUG UPLOAD: wrote ${finalFile.length()} bytes " +
                                        "to ${finalFile.absolutePath}"
                            )

                            uploadedFile = finalFile
                        }

                    } finally {

                        part.release()
                    }
                }

                // ----------------------------------
                // Make sure a file was actually uploaded
                // ----------------------------------

                if (uploadedFile == null) {

                    throw IllegalArgumentException(
                        "No image file was supplied"
                    )
                }

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "status" to "OK",
                        "message" to
                                "Product image uploaded successfully",
                        "imageUrl" to
                                "/api/products/$id/image"
                    )
                )

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid image upload"
                                )
                    )
                )

            } catch (e: NoSuchElementException) {

                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Product not found"
                                )
                    )
                )
            }
        }

        // --------------------------------------
        // DEACTIVATE PRODUCT
        // --------------------------------------

        delete("/api/products/{id}") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.MANAGE_PRODUCTS
                )
            } catch (e: IllegalAccessException) {
                return@delete call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Access denied")
                    )
                )
            }

            try {

                val id =
                    call.parameters["id"]
                        ?.toLongOrNull()
                        ?: throw IllegalArgumentException(
                            "Invalid product ID"
                        )

                productService.deactivateProduct(id)

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "status" to "OK",
                        "message" to "Product deactivated"
                    )
                )

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid product ID"
                                )
                    )
                )

            } catch (e: NoSuchElementException) {

                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Product not found"
                                )
                    )
                )
            }
        }
    }
}

// ==========================================
// PRODUCT -> API RESPONSE
// ==========================================

private fun ProductRecord.toResponse():
        ProductResponse {

    return ProductResponse(
        id = id,
        name = name,
        description = description,
        category = category,
        unit = unit,
        price = price.toPlainString(),
        stockQuantity =
            stockQuantity.toPlainString(),
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        imageUrl =
            "/api/products/$id/image"
    )
}

// ==========================================
// FIND PRODUCT IMAGE
// ==========================================

private fun findProductImage(
    productId: Long
): File? {

    return listOf(
        "jpg",
        "jpeg",
        "png",
        "webp"
    )
        .asSequence()
        .map { extension ->
            File(
                PRODUCT_IMAGE_DIRECTORY,
                "$productId.$extension"
            )
        }
        .firstOrNull { file ->
            file.exists() &&
                    file.isFile &&
                    file.length() > 0L
        }
}

// ==========================================
// DELETE EXISTING PRODUCT IMAGES
// ==========================================

private fun deleteExistingProductImages(
    productId: Long
) {

    listOf(
        "jpg",
        "jpeg",
        "png",
        "webp"
    ).forEach { extension ->

        val file =
            File(
                PRODUCT_IMAGE_DIRECTORY,
                "$productId.$extension"
            )

        if (file.exists()) {
            file.delete()
        }
    }
}