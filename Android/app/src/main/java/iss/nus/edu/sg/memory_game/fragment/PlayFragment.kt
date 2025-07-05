package iss.nus.edu.sg.memory_game.fragment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import iss.nus.edu.sg.memory_game.R
import iss.nus.edu.sg.memory_game.adapter.CardAdapter
import iss.nus.edu.sg.memory_game.apis.RetrofitClient
import iss.nus.edu.sg.memory_game.dao.ScoreRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class PlayFragment : Fragment() {
    private lateinit var cardRecycler: RecyclerView
    private lateinit var matchCounter: TextView
    private lateinit var timer: TextView
    private lateinit var adView: FrameLayout
    private lateinit var soundPool: SoundPool
    private lateinit var mediaPlayer: MediaPlayer
    private var sounderror: Int = 0
    private var soundflip: Int = 0
    private var soundflipback: Int = 0
    private var soundmatchmusic: Int = 0
    private var soundwin: Int = 0

    private val imagePathList = mutableListOf<String>()
    private val bitmapCache = mutableMapOf<String, Bitmap>()
    private var cardAdapter: CardAdapter? = null

    private var bgmPlayer: MediaPlayer?=null
    private var firstPosition: Int? = null
    private var matchedCount = 0
    private var isFlipping = false
    private var gameStarted = false
    private var seconds = 0
    private var runningTimer = false
    private val handler = Handler(Looper.getMainLooper())
    private val runTimer = object : Runnable {
        override fun run() {
            if (runningTimer) {
                seconds++
                updateTimerText(seconds)
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_play, container, false)
        //initialising views to be used
        cardRecycler = view.findViewById<RecyclerView>(R.id.cardRecycler)
        matchCounter = view.findViewById(R.id.matchCounter)
        timer = view.findViewById(R.id.timer)
        adView = view.findViewById(R.id.adView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //install soundpool
        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .build()
        sounderror = soundPool.load(context, R.raw.error, 1)
        soundflip = soundPool.load(context, R.raw.flip, 1)
        soundflipback = soundPool.load(context, R.raw.flip_back, 1)
        soundmatchmusic = soundPool.load(context, R.raw.match_music, 1)

        //initalising music, counter, best time and timer
        startBGM()
        updateMatchCounter()
        updateTimerText(0)

        //retrieving loginPrefs for ad visibility
        val loginPrefs = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val isPaidUser = loginPrefs.getBoolean("isPaidUser", false)
        if (!isPaidUser) {
            childFragmentManager.beginTransaction().replace(R.id.adView, AdFragment()).commit()
        } else {
            adView.visibility = View.GONE
        }

        prepareGameImages()
    }

    private fun prepareGameImages() {
        imagePathList.clear()
        val context = requireContext()
        //retrieving the images path from cache, storing it into a mutable list (imagePathList)
        for (i in 1..6) {
            val path = File(context.cacheDir, "image_$i.jpg").absolutePath
            imagePathList.add(path)
            val file = File(path)
            Log.d("PlayFragment", "Image_$i exists: ${file.exists()}")
        }
        //duplicating images and then shuffle here
        imagePathList.addAll(imagePathList)
        imagePathList.shuffle()
        preloadBitmapsAndSetupRecycler()
    }

    private fun preloadBitmapsAndSetupRecycler() {
        // retrieve the full width of the device's screen in pixels, then divide by 3
        //cos 3 columns, 24 is just for buffer. this will give an optimal size per image
        val size = resources.displayMetrics.widthPixels / 3 - 24

        //using coroutines here for better lightweight and smoother performance than thread.
        //coroutines will be tied to the lifecycle of view (lifecycleScope)
        lifecycleScope.launch(Dispatchers.IO) {
            for (path in imagePathList.distinct()) {
                //decode images here, store it in bitmap
                val bitmap = decodeBitmap(path, size, size)
                if (bitmap != null) {
                    //add in the bitmap (decoded image) into the mutable arraylist
                    bitmapCache[path] = bitmap
                } else {
                    Log.w("PlayFragment", "Failed to decode bitmap for: $path")
                }
            }
            withContext(Dispatchers.Main) {
                setupRecycler()
            }
        }
    }

    private fun setupRecycler() {
        cardAdapter = CardAdapter(imagePathList, bitmapCache) { position, path, imageView ->

            if (isFlipping || imageView.tag == "matched" || !imageView.isEnabled) {
                soundPool.play(sounderror, 1f, 1f, 1, 0, 1f)
                return@CardAdapter
            }

            if (!gameStarted) {
                gameStarted = true
                startTimer()
            }

            imageView.setImageBitmap(bitmapCache[path])

            if (firstPosition == null) {
                firstPosition = position
                isFlipping = true
                handler.postDelayed({
                    isFlipping = false
                }, 300)
                imageView.isEnabled = false
                imageView.isClickable = false
            } else {
                isFlipping = true
                val first = firstPosition!!
                val second = position
                val firstPath = imagePathList[first]
                val secondPath = imagePathList[second]

                if (firstPath == secondPath && first != second) {
                    soundPool.play(soundmatchmusic, 1f, 1f, 1, 0, 1f)

                    val firstViewHolder = cardRecycler.findViewHolderForAdapterPosition(first)
                    if (firstViewHolder is CardAdapter.CardViewHolder) {
                        cardAdapter?.markAsMatched(firstViewHolder.imageView, first)
                    }
                    cardAdapter?.markAsMatched(imageView, second)
                    cardAdapter?.notifyItemChanged(first)
                    cardAdapter?.notifyItemChanged(second)

                    handler.postDelayed({
                        matchedCount++
                        updateMatchCounter()
                        if (matchedCount == 6) {
                            stopTimer()
                            Toast.makeText(context, "All matched! Congratulation!", Toast.LENGTH_SHORT).show()
                            //LST: add the addScore funtion
                            addScoreWithRetrofit(seconds)
                            val mediaPlayer = MediaPlayer.create(context, R.raw.win)
                            mediaPlayer.setOnCompletionListener {
                                val action = PlayFragmentDirections.actionPlayToLeaderboard(seconds)
                                view?.findNavController()?.navigate(action)
                                mediaPlayer.release()
                            }
                            mediaPlayer.start()
                        }
                        isFlipping = false
                        firstPosition = null
                    }, 500)
                } else {
                    soundPool.play(soundflipback, 1f, 1f, 1, 0, 1f)
                    handler.postDelayed({
                        cardAdapter?.hideImage(imageView)
                        val firstViewHolder = cardRecycler.findViewHolderForAdapterPosition(first)
                        if (firstViewHolder is CardAdapter.CardViewHolder) {
                            cardAdapter?.hideImage(firstViewHolder.imageView)
                            firstViewHolder.imageView.isEnabled = true
                            firstViewHolder.imageView.isClickable = true
                        }
                        isFlipping = false
                        firstPosition = null
                    }, 500)
                }
            }
        }

        cardRecycler.layoutManager = GridLayoutManager(requireContext(), 3)
        cardRecycler.adapter = cardAdapter
        cardRecycler.invalidate()
    }

    private fun decodeBitmap(path: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            //decode only image size first
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)

        //resize or shrink image based on target size
        val widthRatio = options.outWidth / targetWidth
        val heightRatio = options.outHeight / targetHeight
        val scale = maxOf(1, minOf(widthRatio, heightRatio))
        options.inSampleSize = scale

        // deecode actual bitmap
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }

    private fun updateMatchCounter() {
        matchCounter.text = "$matchedCount / 6 matches"
    }

    private fun updateTimerText(seconds: Int) {
        val minutes = seconds / 60
        val secs = seconds % 60
        timer.text = String.format("%02d:%02d", minutes, secs)
    }

    private fun startTimer() {
        if (!runningTimer) {
            runningTimer = true
            handler.post(runTimer)
        }
    }

    private fun stopTimer() {
        runningTimer = false
        handler.removeCallbacks(runTimer)
    }
    private fun startBGM() {
        bgmPlayer = MediaPlayer.create(requireContext(), R.raw.bgm)
        bgmPlayer?.isLooping = true
        bgmPlayer?.start()
    }

    private fun stopBGM() {
        bgmPlayer?.stop()
        bgmPlayer?.release()
        bgmPlayer= null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        soundPool.release()
        stopBGM()
    }

    //LST: add score to db
    private fun addScoreWithRetrofit(seconds: Int) {
        val loginPrefs = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val userId = loginPrefs.getString("UserID", null) ?: return


        val scoreRequest = ScoreRequest(userId = userId, time = seconds)

        val call = RetrofitClient.scoreApi.addScore(scoreRequest)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Score uploaded!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to upload score!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
