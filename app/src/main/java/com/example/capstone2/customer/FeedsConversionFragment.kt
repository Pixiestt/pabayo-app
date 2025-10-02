package com.example.capstone2.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.capstone2.R

class FeedsConversionFragment : Fragment() {
    
    private lateinit var radioGroupFeeds: RadioGroup
    private lateinit var radioFeedsYes: RadioButton
    private lateinit var radioFeedsNo: RadioButton
    private lateinit var btnNext: Button
    private lateinit var tvStepProgress: TextView
    
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
        btnNext = view.findViewById(R.id.btnNext)
        tvStepProgress = view.findViewById(R.id.tvStepProgress)
        
        val activity = requireActivity() as RequestWizardActivity
        tvStepProgress.text = "4/5"

        // Initially disable Next until user explicitly chooses Yes or No
        btnNext.isEnabled = false
        btnNext.alpha = 0.5f

        // Prefill from wizard data only if already set (e.g., editing an existing request)
        val wizard = activity.getWizardData()
        if (wizard.feedsConversion != null) {
            val isChecked = wizard.feedsConversion == true
            if (isChecked) radioFeedsYes.isChecked = true else radioFeedsNo.isChecked = true
            // enable Next because a choice exists
            btnNext.isEnabled = true
            btnNext.alpha = 1.0f
        }

        // Set up radio group listener: enable Next and write choice into wizard data
        radioGroupFeeds.setOnCheckedChangeListener { _, checkedId ->
            val selected = when (checkedId) {
                R.id.radioFeedsYes -> true
                R.id.radioFeedsNo -> false
                else -> null
            }
            // only enable Next when a valid selection (Yes/No) is made
            if (selected != null) {
                activity.getWizardData().feedsConversion = selected
                btnNext.isEnabled = true
                btnNext.alpha = 1.0f
            } else {
                btnNext.isEnabled = false
                btnNext.alpha = 0.5f
            }
        }
        
        btnNext.setOnClickListener {
            activity.goToNextStep()
        }
    }
}
