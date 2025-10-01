package com.example.capstone2.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.capstone2.R

class FeedsConversionFragment : Fragment() {
    
    private lateinit var switchFeedsConversion: Switch
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
        
        switchFeedsConversion = view.findViewById(R.id.switchFeedsConversion)
        btnNext = view.findViewById(R.id.btnNext)
        tvStepProgress = view.findViewById(R.id.tvStepProgress)
        
        val activity = requireActivity() as RequestWizardActivity
        tvStepProgress.text = "4/5"

        // Prefill from wizard data if present
        val wizard = activity.getWizardData()
        wizard.feedsConversion?.let { isChecked ->
            switchFeedsConversion.isChecked = isChecked
            activity.getWizardData().feedsConversion = isChecked
        }

        // Set up switch listener
        switchFeedsConversion.setOnCheckedChangeListener { _, isChecked ->
            activity.getWizardData().feedsConversion = isChecked
        }
        
        btnNext.setOnClickListener {
            activity.goToNextStep()
        }
    }
}
