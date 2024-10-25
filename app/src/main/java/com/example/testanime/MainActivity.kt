package com.example.testanime

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.Surface
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.testanime.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val imagesList = ArrayList<Uri>()
    private lateinit var imagesAdapter: ImagesAdapter
    private var frameDurationMs: Long = 1000 // Длительность кадра в миллисекундах по умолчанию
    private var frameRate: Int = 30 // Частота кадров по умолчанию
    private var videoFile: File? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Запрос разрешений
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imagesAdapter = ImagesAdapter(imagesList)
        binding.recyclerViewImages.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewImages.adapter = imagesAdapter

        // Инициализация Spinner
        val frameRateSpinner = binding.spinnerFrameRate

        // Получение массива значений из ресурсов
        val frameRateOptions = resources.getStringArray(R.array.frame_rate_options)

        // Создание адаптера для Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, frameRateOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        frameRateSpinner.adapter = adapter

        // Установка обработчика выбора элемента
        frameRateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                frameRate = frameRateOptions[position].toInt()
                createVideo()
                Toast.makeText(
                    this@MainActivity,
                    "Частота кадров установлена: $frameRate fps",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Ничего не делаем
            }
        }

        // Позволяет перемещать элементы RecyclerView
        val itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewImages)

        binding.buttonSelectImages.setOnClickListener {
            selectImages()
        }



        binding.buttonSaveVideo.setOnClickListener {
            saveVideoToGallery()
        }

        binding.buttonPlayVideo.setOnClickListener {
            playVideo()
        }

        binding.buttonDuplicateImages.setOnClickListener {
            showDuplicateImagesDialog()
        }
    }

    private val selectImagesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        onImagesSelected(result.resultCode, result.data)
    }

    private fun selectImages() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        selectImagesLauncher.launch(Intent.createChooser(intent, "Выберите изображения"))
    }

    private fun onImagesSelected(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            imagesList.clear()
            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    imagesList.add(imageUri)
                }
            } else if (data?.data != null) {
                val imageUri = data.data!!
                imagesList.add(imageUri)
            }
            imagesAdapter.notifyDataSetChanged()
            createVideo()
        }
    }

    // Реализация перемещения элементов в RecyclerView
    private val simpleCallback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
        0
    ) {
        override fun onMove(
            recyclerView: androidx.recyclerview.widget.RecyclerView,
            viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
            target: androidx.recyclerview.widget.RecyclerView.ViewHolder
        ): Boolean {
            val fromPosition = viewHolder.bindingAdapterPosition
            val toPosition = target.bindingAdapterPosition
            val movedItem = imagesList.removeAt(fromPosition)
            imagesList.add(toPosition, movedItem)
            imagesAdapter.notifyItemMoved(fromPosition, toPosition)
            createVideo()
            return true
        }

        override fun onSwiped(
            viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
            direction: Int
        ) {
            // Не требуется
        }
    }

    // Метод для ввода длительности кадра пользователем
    private fun changeFrameDuration() {
        val editText = EditText(this)
        editText.hint = "Введите длительность кадра (мс)"
        editText.inputType = InputType.TYPE_CLASS_NUMBER

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Изменить длительность кадра")
        builder.setView(editText)
        builder.setPositiveButton("OK") { _, _ ->
            val input = editText.text.toString()
            if (input.isNotEmpty()) {
                val newFrameDuration = input.toLongOrNull()
                if (newFrameDuration != null && newFrameDuration > 0) {
                    frameDurationMs = newFrameDuration
                    createVideo()
                    Toast.makeText(
                        this,
                        "Длительность кадра установлена: $frameDurationMs мс",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(this, "Введите корректное число", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Поле не должно быть пустым", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Отмена", null)
        builder.show()
    }

    private fun createVideo() {
        if (imagesList.isEmpty()) return

        Thread {
            try {
                val width = 720
                val height = 1280

                val format = MediaFormat.createVideoFormat("video/avc", width, height)
                format.setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                )
                format.setInteger(MediaFormat.KEY_BIT_RATE, 5000000)
                format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

                val encoder = MediaCodec.createEncoderByType("video/avc")
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                val surface = encoder.createInputSurface()
                encoder.start()

                val path = getExternalFilesDir(null)
                videoFile = File(path, "myvideo.mp4")
                if (videoFile!!.exists()) {
                    videoFile!!.delete()
                }
                val muxer = MediaMuxer(
                    videoFile!!.absolutePath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                )

                var trackIndex = -1
                var isMuxerStarted = false
                val bufferInfo = MediaCodec.BufferInfo()

                var totalFrames = 0
                val durationPerFrameUs = 1_000_000.0 / frameRate // Длительность одного кадра в микросекундах

                // Расчёт количества кадров для каждого изображения
                val framesPerImage = (frameDurationMs * frameRate / 1000).toInt()

                for (uri in imagesList) {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)

                    // Вычисляем коэффициент масштабирования
                    val scaleX = if (bitmap.width > width) width.toFloat() / bitmap.width else 1f
                    val scaleY = if (bitmap.height > height) height.toFloat() / bitmap.height else 1f
                    val scale = minOf(scaleX, scaleY)

                    val scaledWidth = (bitmap.width * scale).toInt()
                    val scaledHeight = (bitmap.height * scale).toInt()

                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

                    for (i in 0 until framesPerImage) {
                        val canvas = surface.lockCanvas(null)

                        // Очищаем канву
                        canvas.drawColor(Color.BLACK)

                        // Центрируем изображение
                        val left = (width - scaledWidth) / 2f
                        val top = (height - scaledHeight) / 2f

                        canvas.drawBitmap(scaledBitmap, left, top, null)
                        surface.unlockCanvasAndPost(canvas)

                        // Обработка выходных буферов для текущего кадра
                        var encoderOutputAvailable = true
                        while (encoderOutputAvailable) {
                            val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 0)
                            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                // Выходные буферы пока недоступны
                                break
                            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                if (isMuxerStarted) {
                                    throw RuntimeException("Format changed twice")
                                }
                                val newFormat = encoder.outputFormat
                                trackIndex = muxer.addTrack(newFormat)
                                muxer.start()
                                isMuxerStarted = true
                            } else if (encoderStatus >= 0) {
                                val encodedData = encoder.getOutputBuffer(encoderStatus)
                                    ?: throw RuntimeException("Encoder output buffer $encoderStatus was null")

                                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                    bufferInfo.size = 0
                                }

                                if (bufferInfo.size != 0) {
                                    if (!isMuxerStarted) {
                                        throw RuntimeException("Muxer hasn't started")
                                    }

                                    // Устанавливаем точную временную метку
                                    bufferInfo.presentationTimeUs = (totalFrames * durationPerFrameUs).toLong()

                                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo)

                                    // Увеличиваем счётчик кадров
                                    totalFrames += 1
                                }

                                encoder.releaseOutputBuffer(encoderStatus, false)

                                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                    encoderOutputAvailable = false
                                }
                            }
                        }
                    }

                    scaledBitmap.recycle()
                    bitmap.recycle()
                }

                // Завершаем кодирование
                encoder.signalEndOfInputStream()

                // Обработка оставшихся выходных буферов
                var encoderOutputAvailable = true
                while (encoderOutputAvailable) {
                    val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                    if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // Нет доступных буферов
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (isMuxerStarted) {
                            throw RuntimeException("Format changed twice")
                        }
                        val newFormat = encoder.outputFormat
                        trackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        isMuxerStarted = true
                    } else if (encoderStatus >= 0) {
                        val encodedData = encoder.getOutputBuffer(encoderStatus)
                            ?: throw RuntimeException("Encoder output buffer $encoderStatus was null")

                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }

                        if (bufferInfo.size != 0) {
                            if (!isMuxerStarted) {
                                throw RuntimeException("Muxer hasn't started")
                            }

                            bufferInfo.presentationTimeUs = (totalFrames * durationPerFrameUs).toLong()
                            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }

                        encoder.releaseOutputBuffer(encoderStatus, false)

                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            encoderOutputAvailable = false
                        }
                    }
                }

                encoder.stop()
                encoder.release()
                muxer.stop()
                muxer.release()

                runOnUiThread {
                    Toast.makeText(this, "Видео создано", Toast.LENGTH_SHORT).show()
                    binding.videoView.setVideoURI(Uri.fromFile(videoFile))
                    binding.videoView.visibility = View.VISIBLE
                    binding.videoView.start()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Ошибка создания видео: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }.start()
    }


    private fun saveVideoToGallery() {
        if (videoFile == null || !videoFile!!.exists()) {
            Toast.makeText(this, "Видео не создано", Toast.LENGTH_SHORT).show()
            return
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "myvideo_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/MyAppVideos")
        }

        val uri =
            contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            val outputStream = contentResolver.openOutputStream(uri)
            val inputStream = videoFile!!.inputStream()
            outputStream?.use { out ->
                inputStream.use { input ->
                    input.copyTo(out)
                }
            }
            Toast.makeText(this, "Видео сохранено в галерее", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Ошибка сохранения видео", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playVideo() {
        if (videoFile == null || !videoFile!!.exists()) {
            Toast.makeText(this, "Видео не создано", Toast.LENGTH_SHORT).show()
            return
        }
        binding.videoView.setVideoURI(Uri.fromFile(videoFile))
        binding.videoView.visibility = View.VISIBLE
        binding.videoView.start()
    }

    // Метод для дублирования изображений с выбором
    private fun showDuplicateImagesDialog() {
        if (imagesList.isEmpty()) {
            Toast.makeText(this, "Список изображений пуст", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_select_images, null)
        val recyclerView =
            dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewSelectImages)
        val duplicateAdapter = SelectableImagesAdapter(imagesList)

        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 3)
        recyclerView.adapter = duplicateAdapter

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Выберите изображения для дублирования")
        builder.setView(dialogView)
        builder.setPositiveButton("Дублировать") { _, _ ->
            val selectedImages = duplicateAdapter.getSelectedImages()
            if (selectedImages.isNotEmpty()) {
                imagesList.addAll(selectedImages)
                imagesAdapter.notifyItemRangeInserted(
                    imagesList.size - selectedImages.size,
                    selectedImages.size
                )
                createVideo()
            } else {
                Toast.makeText(this, "Вы не выбрали изображения", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Отмена", null)
        builder.show()
    }
}
