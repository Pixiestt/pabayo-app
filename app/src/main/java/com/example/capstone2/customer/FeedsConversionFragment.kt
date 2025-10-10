package com.example.capstone2.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.example.capstone2.R

class FeedsConversionFragment : Fragment() {
    
    private lateinit var radioGroupFeeds: RadioGroup
    private lateinit var radioFeedsYes: RadioButton
    private lateinit var radioFeedsNo: RadioButton
    private lateinit var btnSubmit: Button
    private lateinit var tvStepProgress: TextView
    private lateinit var etComment: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feeds_conversion, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        radioGroupFeeds = view.findViewById(R.id.radioGroupFeeds)
        radioFeedsYes = view.findViewById(R.id.radioFeedsYes)
        radioFeedsNo = view.findViewById(R.id.radioFeedsNo)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        tvStepProgress = view.findViewById(R.id.tvStepProgress)
        etComment = view.findViewById(R.id.etComment)

        val activity = requireActivity() as RequestWizardActivity
        tvStepProgress.text = "4/4"

        // Initially disable Submit until user explicitly chooses Yes or No
        btnSubmit.isEnabled = false
        btnSubmit.alpha = 0.5f

        // Prefill from wizard data only if already set (e.g., editing an existing request)
        val wizard = activity.getWizardData()
        if (wizard.feedsConversion != null) {
            val isChecked = wizard.feedsConversion == true
            if (isChecked) radioFeedsYes.isChecked = true else radioFeedsNo.isChecked = true
            // enable Submit because a choice exists
            btnSubmit.isEnabled = true
            btnSubmit.alpha = 1.0f
        }

        // Prefill comment if present
        wizard.comment?.let { etComment.setText(it) }

        // Set up radio group listener: enable Submit and write choice into wizard data
        radioGroupFeeds.setOnCheckedChangeListener { _, checkedId ->
            val selected = when (checkedId) {
                R.id.radioFeedsYes -> true
                R.id.radioFeedsNo -> false
                else -> null
            }
            // only enable Submit when a valid selection (Yes/No) is made
            if (selected != null) {
                activity.getWizardData().feedsConversion = selected
                btnSubmit.isEnabled = true
                btnSubmit.alpha = 1.0f
            } else {
                btnSubmit.isEnabled = false
                btnSubmit.alpha = 0.5f
            }
        }
        
        btnSubmit.setOnClickListener {
            // Save optional comment into wizard data
            activity.getWizardData().comment = etComment.text.toString().trim()
            // Final step: submit the request
            activity.submitRequest()
        }
    }
}
