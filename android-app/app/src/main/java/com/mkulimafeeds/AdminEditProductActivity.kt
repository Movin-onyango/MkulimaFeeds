package com.mkulimafeeds

import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope

import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.ProductRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminEditProductActivity : AppCompatActivity() {

    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var etName: TextInputEditText
    private lateinit var etSku: TextInputEditText
    private lateinit var etPrice: TextInputEditText
    private lateinit var etStock: TextInputEditText
    private lateinit var etUnit: TextInputEditText
    private lateinit var etDesc: TextInputEditText

    private lateinit var spinnerCategory: AutoCompleteTextView

    private lateinit var tvTitle: TextView
    private lateinit var btnSave: MaterialButton

    private lateinit var cardAddImage: View
    private lateinit var ivProductPreview: ImageView

    // =========================================================
    // REPOSITORY / AUTH
    // =========================================================

    private lateinit var productRepository: ProductRepository
    private lateinit var tokenManager: TokenManager

    // =========================================================
    // STATE
    // =========================================================

    private var isEditMode = false
    private var productId: String? = null

    private var selectedImageUri: Uri? = null

    // =========================================================
    // IMAGE PICKER
    // =========================================================

    private val imagePicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                selectedImageUri = uri

                ivProductPreview.setImageURI(uri)

                Toast.makeText(
                    this,
                    "Product image selected",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_admin_edit_product
        )

        val rootView =
            findViewById<View>(
                R.id.edit_product_root
            )

        ViewCompat.setOnApplyWindowInsetsListener(
            rootView
        ) { v, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            v.setPadding(
                0,
                0,
                0,
                systemBars.bottom
            )

            insets
        }

        initializeViews()

        productRepository =
            ProductRepository(
                NetworkModule.apiService
            )

        tokenManager =
            TokenManager(
                applicationContext
            )

        setupCategorySpinner()

        // -----------------------------------------------------
        // IMAGE SELECTION
        // -----------------------------------------------------

        cardAddImage.setOnClickListener {

            imagePicker.launch(
                "image/*"
            )
        }

        // -----------------------------------------------------
        // EDIT MODE
        // -----------------------------------------------------

        isEditMode =
            intent.getBooleanExtra(
                "IS_EDIT",
                false
            )

        productId =
            intent.getStringExtra(
                "PRODUCT_ID"
            )

        if (
            isEditMode &&
            productId != null
        ) {

            tvTitle.text =
                getString(
                    R.string.admin_edit_product_title
                )

            loadProduct(
                productId!!
            )
        }

        // -----------------------------------------------------
        // BACK
        // -----------------------------------------------------

        findViewById<ImageView>(
            R.id.btnBackEdit
        ).setOnClickListener {

            finish()
        }

        // -----------------------------------------------------
        // SAVE
        // -----------------------------------------------------

        btnSave.setOnClickListener {

            saveProduct()
        }
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private fun initializeViews() {

        etName =
            findViewById(
                R.id.etProductName
            )

        etSku =
            findViewById(
                R.id.etProductSku
            )

        etPrice =
            findViewById(
                R.id.etProductPrice
            )

        etStock =
            findViewById(
                R.id.etProductStock
            )

        etUnit =
            findViewById(
                R.id.etProductUnit
            )

        etDesc =
            findViewById(
                R.id.etProductDesc
            )

        spinnerCategory =
            findViewById(
                R.id.spinnerCategory
            )

        tvTitle =
            findViewById(
                R.id.tvEditTitle
            )

        btnSave =
            findViewById(
                R.id.btnSaveProduct
            )

        cardAddImage =
            findViewById(
                R.id.cardAddImage
            )

        ivProductPreview =
            findViewById(
                R.id.ivProductPreview
            )
    }

    // =========================================================
    // CATEGORY
    // =========================================================

    private fun setupCategorySpinner() {

        val defaultCategories =
            listOf(
                "Poultry",
                "Dairy",
                "Pig",
                "Supplements",
            )

        val defaultAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                defaultCategories
            )

        spinnerCategory.setAdapter(
            defaultAdapter
        )

        // IMPORTANT:
        // The field is editable, so the administrator
        // can type a completely new category.

        spinnerCategory.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS

        // -----------------------------------------------------
        // LOAD CATEGORIES ALREADY USED BY PRODUCTS
        // -----------------------------------------------------

        lifecycleScope.launch {

            try {

                val backendCategories =
                    productRepository
                        .getProducts()
                        .map {
                            it.category.trim()
                        }
                        .filter {
                            it.isNotBlank()
                        }

                val categories =
                    (
                            defaultCategories +
                                    backendCategories
                            )
                        .distinct()
                        .sorted()

                val dynamicAdapter =
                    ArrayAdapter(
                        this@AdminEditProductActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        categories
                    )

                spinnerCategory.setAdapter(
                    dynamicAdapter
                )

            } catch (_: Exception) {

                // Keep default suggestions
                // when backend is unavailable.
            }
        }
    }

    // =========================================================
    // LOAD PRODUCT
    // =========================================================

    private fun loadProduct(
        id: String
    ) {

        val backendId =
            id.toLongOrNull()

        if (backendId == null) {

            Toast.makeText(
                this,
                "Invalid product ID",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        setLoading(true)

        lifecycleScope.launch {

            try {

                val product =
                    productRepository.getProduct(
                        backendId
                    )

                etName.setText(
                    product.name
                )

                etSku.setText(
                    product.sku
                )

                etPrice.setText(
                    product.price.toString()
                )

                etStock.setText(
                    product.stock.toString()
                )

                etUnit.setText(
                    product.unit
                )

                spinnerCategory.setText(
                    product.category,
                    false
                )

                etDesc.setText(
                    product.description
                )

            } catch (e: Exception) {

                Toast.makeText(
                    this@AdminEditProductActivity,
                    "Failed to load product: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                finish()

            } finally {

                setLoading(false)
            }
        }
    }

    // =========================================================
    // SAVE PRODUCT
    // =========================================================

    private fun saveProduct() {

        val name =
            etName.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val sku =
            etSku.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val priceStr =
            etPrice.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val stockStr =
            etStock.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val unit =
            etUnit.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val category =
            spinnerCategory.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val desc =
            etDesc.text
                ?.toString()
                ?.trim()
                .orEmpty()

        // -----------------------------------------------------
        // REQUIRED FIELDS
        // -----------------------------------------------------

        if (
            name.isEmpty() ||
            priceStr.isEmpty() ||
            stockStr.isEmpty() ||
            unit.isEmpty() ||
            category.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Please complete all required product fields",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // -----------------------------------------------------
        // PRICE
        // -----------------------------------------------------

        val price =
            priceStr.toDoubleOrNull()

        if (
            price == null ||
            price < 0
        ) {

            Toast.makeText(
                this,
                "Please enter a valid price",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // -----------------------------------------------------
        // STOCK
        // -----------------------------------------------------

        val stock =
            stockStr.toIntOrNull()

        if (
            stock == null ||
            stock < 0
        ) {

            Toast.makeText(
                this,
                "Please enter a valid stock quantity",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // -----------------------------------------------------
        // TOKEN
        // -----------------------------------------------------

        val token =
            tokenManager.getToken()

        if (
            token.isNullOrBlank()
        ) {

            Toast.makeText(
                this,
                "Your session has expired. Please log in again.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        setLoading(true)

        lifecycleScope.launch {

            try {

                var backendId: Long

                // =================================================
                // CREATE / UPDATE PRODUCT
                // =================================================

                if (
                    isEditMode &&
                    productId != null
                ) {

                    backendId =
                        productId!!
                            .toLongOrNull()
                            ?: throw IllegalArgumentException(
                                "Invalid product ID"
                            )

                    productRepository.updateProduct(
                        id = backendId,
                        name = name,
                        description = desc,
                        category = category,
                        unit = unit,
                        price = price,
                        stockQuantity = stock,
                        token = token
                    )

                } else {

                    val createdProduct =
                        productRepository.createProduct(
                            name = name,
                            description = desc,
                            category = category,
                            unit = unit,
                            price = price,
                            stockQuantity = stock,
                            token = token
                        )

                    backendId =
                        createdProduct.id
                            .toLongOrNull()
                            ?: throw IllegalStateException(
                                "Product was created but its ID could not be read"
                            )
                }

                // =================================================
                // IMAGE UPLOAD
                // =================================================

                var imageFailed = false

                selectedImageUri?.let { uri ->

                    try {

                        val imageBytes =
                            withContext(
                                Dispatchers.IO
                            ) {

                                contentResolver
                                    .openInputStream(uri)
                                    ?.use { inputStream ->

                                        inputStream.readBytes()
                                    }
                            }

                        if (
                            imageBytes == null ||
                            imageBytes.isEmpty()
                        ) {

                            imageFailed = true

                        } else {

                            val fileName =
                                uri.lastPathSegment
                                    ?.substringAfterLast("/")
                                    ?.ifBlank {
                                        "product_image.jpg"
                                    }
                                    ?: "product_image.jpg"

                            // Resolve MIME type: prefer the resolver's
                            // value; fall back to a sensible default
                            // so the backend can always resolve an
                            // extension.
                            val mimeType =
                                contentResolver.getType(uri)
                                    ?.takeIf { it.isNotBlank() }
                                    ?: "image/jpeg"

                            Log.d(
                                "ImageUpload",
                                "Uploading: fileName=$fileName, " +
                                        "mimeType=$mimeType, " +
                                        "bytes=${imageBytes.size}"
                            )

                            productRepository.uploadProductImage(
                                id = backendId,
                                imageBytes = imageBytes,
                                fileName = fileName,
                                mimeType = mimeType,
                                token = token
                            )
                        }

                    } catch (e: Exception) {

                        e.printStackTrace()

                        Log.e(
                            "ImageUpload",
                            "Upload failed",
                            e
                        )

                        imageFailed = true
                    }
                }

                // =================================================
                // RESULT
                // =================================================

                if (imageFailed) {

                    Toast.makeText(
                        this@AdminEditProductActivity,
                        "Product saved, but image upload failed",
                        Toast.LENGTH_LONG
                    ).show()

                } else {

                    Toast.makeText(
                        this@AdminEditProductActivity,
                        if (isEditMode) {
                            "Product updated successfully"
                        } else {
                            "Product created successfully"
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }

                finish()

            } catch (e: Exception) {

                Toast.makeText(
                    this@AdminEditProductActivity,
                    "Failed to save product: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                setLoading(false)
            }
        }
    }

    // =========================================================
    // LOADING STATE
    // =========================================================

    private fun setLoading(
        loading: Boolean
    ) {

        btnSave.isEnabled =
            !loading

        btnSave.text =
            if (loading) {
                "Saving..."
            } else {
                "Save Product"
            }

        cardAddImage.isEnabled =
            !loading
    }
}