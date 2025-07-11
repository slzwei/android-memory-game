package iss.nus.edu.sg.memory_game.fragment


import android.os.Bundle
import android.os.Handler
import android.os.Looper
import kotlin.random.Random
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import androidx.fragment.app.Fragment
import iss.nus.edu.sg.memory_game.R

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import iss.nus.edu.sg.memory_game.apis.RetrofitClient
import android.util.Log


class AdFragment : Fragment() {
    private lateinit var adImageView: ImageView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var adRunnable: Runnable


    private var adImageUrls: List<String> = emptyList()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_ad, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adImageView = view.findViewById(R.id.adImageView)

        //launching coroutine here cos calling API to retrieve ad images
        lifecycleScope.launch {
            adImageUrls = RetrofitClient.adApi.getAdUrls()

            if (adImageUrls.isNotEmpty()) {
                showRandomAd()

                adRunnable = object : Runnable {
                    override fun run() {
                        showRandomAd()
                        handler.postDelayed(this, 30_000)
                    }
                }
                handler.postDelayed(adRunnable, 30_000)
            }
        }
    }

    private fun showRandomAd() {

        if (adImageUrls.isNotEmpty()) {
            val selectedAdUrl = adImageUrls.random()

            Glide.with(requireContext())
                .load(selectedAdUrl)
                .into(adImageView)

        } else {
            adImageView.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(adRunnable)
    }

    companion object {
        fun newInstance(isPaidUser: Boolean): AdFragment {
            val fragment = AdFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}