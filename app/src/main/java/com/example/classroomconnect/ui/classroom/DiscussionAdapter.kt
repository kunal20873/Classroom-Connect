package com.example.classroomconnect.ui.classroom

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.classroomconnect.R
import com.example.classroomconnect.data.models.Discussion
import java.text.SimpleDateFormat
import java.util.*

sealed class ChatItem {
    data class MessageItem(val discussion: Discussion) : ChatItem()
    data class DateHeader(val date: String) : ChatItem()
}

class DiscussionAdapter(
    var messageList: ArrayList<Discussion>,
    var context: Activity,
    private val currentUserId: String,
    var onDeleteClick: (Discussion) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val chatItems = ArrayList<ChatItem>()

    companion object {
        private const val VIEW_TYPE_DATE_HEADER = 0
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    init {
        updateChatItems(messageList)
    }

    fun setData(newList: List<Discussion>) {
        messageList = ArrayList(newList)
        updateChatItems(messageList)
        notifyDataSetChanged()
    }

    private fun updateChatItems(messages: List<Discussion>) {
        chatItems.clear()
        if (messages.isEmpty()) return

        var lastDate = ""
        for (message in messages) {
            val dateLabel = getFormattedDateHeader(message.timestamp)
            if (dateLabel != lastDate) {
                chatItems.add(ChatItem.DateHeader(dateLabel))
                lastDate = dateLabel
            }
            chatItems.add(ChatItem.MessageItem(message))
        }
    }

    private fun getFormattedDateHeader(timestamp: Long): String {
        val messageCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()

        return when {
            isSameDay(messageCalendar, now) -> "Today"
            isYesterday(messageCalendar, now) -> "Yesterday"
            else -> {
                val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(cal: Calendar, now: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(cal, yesterday)
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderName: TextView? = itemView.findViewById(R.id.tvSenderName)
        val message: TextView = itemView.findViewById(R.id.tvMessageBody)
        val time: TextView = itemView.findViewById(R.id.tvMessageTime)
    }

    class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDateHeader)
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = chatItems[position]) {
            is ChatItem.DateHeader -> VIEW_TYPE_DATE_HEADER
            is ChatItem.MessageItem -> {
                if (item.discussion.senderUid == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DATE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_header, parent, false)
                DateHeaderViewHolder(view)
            }
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false)
                MessageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false)
                MessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = chatItems[position]) {
            is ChatItem.DateHeader -> {
                (holder as DateHeaderViewHolder).tvDate.text = item.date
            }
            is ChatItem.MessageItem -> {
                val messageHolder = holder as MessageViewHolder
                val currentItem = item.discussion

                messageHolder.senderName?.text = currentItem.userNAME
                messageHolder.message.text = currentItem.message

                if (currentItem.timestamp != 0L) {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    messageHolder.time.text = sdf.format(Date(currentItem.timestamp))
                    messageHolder.time.visibility = View.VISIBLE
                } else {
                    messageHolder.time.visibility = View.GONE
                }

                messageHolder.itemView.setOnLongClickListener {
                    onDeleteClick(currentItem)
                    true
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return chatItems.size
    }
}
