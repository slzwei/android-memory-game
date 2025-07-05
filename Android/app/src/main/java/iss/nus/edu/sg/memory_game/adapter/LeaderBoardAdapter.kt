package iss.nus.edu.sg.memory_game.adapter


import android.view.LayoutInflater
import android.view.View
import iss.nus.edu.sg.memory_game.R
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import iss.nus.edu.sg.memory_game.dao.ScoreResult

class LeaderBoardAdapter(private val items: List<ScoreResult>) : RecyclerView.Adapter<LeaderBoardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val usernameText = view.findViewById<TextView>(R.id.usernameText)
        val timeText = view.findViewById<TextView>(R.id.timeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_score_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val score = items[position]
        holder.usernameText.text = score.username
        holder.timeText.text = String.format("%02d:%02d", score.time/60, score.time % 60)
    }

    override fun getItemCount() = items.size

}