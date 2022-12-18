package com.example.traveljournal.ui.documents

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.traveljournal.R
import com.example.traveljournal.databinding.FragmentNewDocumentBinding
import com.example.traveljournal.databinding.FragmentNewTripBinding
import com.example.traveljournal.room.LocalDB
import com.example.traveljournal.room.documents.DocumentEntity
import com.example.traveljournal.room.trips.TripEntity
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class NewDocumentFragment : Fragment() {
    private val TAG = "NewDocumentFragment"
    private var _binding: FragmentNewDocumentBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNewDocumentBinding.inflate(inflater, container, false)
        val root: View = binding.root

        createNotificationChannel()
        setupSaveButton()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupSaveButton() {
        binding.buttonSave.setOnClickListener {
            // Fetch the values from UI user input
            val newDocument = getUserEnteredDocument()
            // Store them in DB
            if (newDocument != null){
                //after saving the new trip, the view navigates back to all Trips
                saveDocumentToDB(newDocument)
                scheduleNotification()
                findNavController().navigate(R.id.action_backToAllDocs)
            } else {
                Toast.makeText(context, "Some fields empty or date invalid", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveDocumentToDB(newDocument: DocumentEntity) {
        context?.let {LocalDB.getDocumentsInstance(it).getDocumentDAO().insertDocument(newDocument)}
    }

    private fun getUserEnteredDocument(): DocumentEntity? {
        val editTexts = listOf(
            binding.editIdType,
            binding.editExpiryDate
        )

        val allEditTextsHaveContent = editTexts.all { !TextUtils.isEmpty(it.text) }
        val parsedDate = parseDate(editTexts[1].text.toString())
        if (!allEditTextsHaveContent or (parsedDate == null)) {
            return null // Input is not valid
        }
        //create a new trip entity based on user entered values
        val document = DocumentEntity(
            0,
            editTexts[0].text.toString(),
            parsedDate
        )
        Log.i(
            TAG,
            " ${document.id} + ${document.type} + ${document.expiration}"
        )
        return document
    }

    /** Tries to parse argument string as a Date in format specified by TripEntity.DATEFORMAT,
     * returns null if fails or a Date object if succeeded */
    private fun parseDate(inDate: String): Date? {
        val dateFormat = SimpleDateFormat(TripEntity.DATEFORMAT, Locale.getDefault())
        dateFormat.isLenient = false
        var parsedDate: Date? = null
        try {
            parsedDate = dateFormat.parse(inDate)
        } catch (pe: ParseException) {
            return parsedDate
        }
        return parsedDate
    }

    /**
     * Notification idea: the app will notify the user at certain times before the document expires.
     * 3 months before, 1 month before and 1 week before. It will also notify the used when it has expired
     */

    private fun scheduleNotification()
    {
        val intent = Intent(context, Notification::class.java)
        val idType = binding.editIdType.text.toString()
        val title = "Document about to expire!"
        val expDate = parseDate(binding.editExpiryDate.text.toString())
        val expDateInMillis: Long = expDate!!.time
        val reminder1: Long = expDateInMillis - 15778800000 //6 months before the exp date
        val reminder2: Long = expDateInMillis - 7889400000 //3 months before the exp date
        val reminder3: Long = expDateInMillis - 2629800000 //1 month before the exp date
        val reminder4: Long = expDateInMillis - 604800016//1 week before the exp date
        val message = "Your document ${idType} will expire in X months."
        intent.putExtra(titleExtra, title)
        intent.putExtra(messageExtra, message)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = activity?.getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            expDateInMillis,
            pendingIntent
        )
    }

    private fun createNotificationChannel()
    {
        val name = "Document Expiration Channel"
        val desc = "To remind user when document is about to expire."
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelID, name, importance)
        channel.description = desc
        val notificationManager = activity?.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}